# Flujo del sistema: LEON BON

---

## 1. Hub de Inicio

Es la pantalla central del usuario. Muestra contexto inmediato sobre el estado de la plataforma y da acceso rápido a todas las zonas.

- **Header (persistente):**

    - **Izquierda:** Logo LEON BON — enlace de regreso al inicio desde cualquier pantalla.
    - **Centro/nav:** Torneos, Equipos, Rankings.
    - **Derecha (sesión):** Enlace al perfil público, acceso a editar perfil, chip **LC** con saldo (enlaza al inicio), botón Salir. En admin: accesos directos a equipos pendientes, lista de torneos y crear torneo.
    - **Derecha (sin sesión):** Botones Entrar y Registro.

- **Contenido del hub (con sesión):**

    - Tarjeta de **resumen de perfil**: número de jugador, usuario, nickname, rol, estado y saldo de L-Coins.
    - Botón de **Daily Claim** (+100 LC) que el usuario ejecuta una vez por ventana diaria.
    - Tabla de **historial económico**: fecha, tipo de movimiento, monto (verde/rojo) y saldo resultante.
    - Accesos rápidos diferenciados para admin.

- **Contenido del hub (sin sesión):** Llamado a entrar o registrarse.

---

## 2. Perfil del Jugador

Toda cuenta tiene un **perfil público** accesible por su ID. Muestra lo que el jugador decide publicar.

- **Datos visibles:** Número LEON, nickname, juego preferido, rango por juego, equipo actual (con enlace), trofeos obtenidos y redes sociales configuradas.

- **Datos privados editables (solo el propio jugador):** Nombre real con control de visibilidad, correo, país, todos los enlaces de redes. Editable desde `/profile/edit`.

- **Estadísticas:** El módulo de plataforma calcula un snapshot del historial de partidas; los leaderboards por juego usan esos datos para los rankings.

---

## 3. Ciclo de Vida del Equipo

### **A. Creación y Aprobación**

1. El jugador rellena el formulario: nombre, tag (siglas), logo, región.
2. El sistema lo marca como `PENDING` y lo convierte en **capitán**.
3. El **admin** revisa en su panel de equipos pendientes y da `Aprobar` o `Rechazar`. También puede suspender o resetear el logo si viola políticas.
4. Al aprobarse, el equipo aparece en el listado público.

### **B. Reclutamiento**

1. Otro jugador entra al detalle del equipo y presiona **"Solicitar unirse"**.
2. El **capitán** ve la solicitud en la pantalla del equipo y decide `Aceptar` o `Rechazar`.

### **C. Traspaso de Capitanía**

- El capitán puede **delegar la capitanía** a otro miembro del roster.
- El nuevo capitán hereda los permisos de gestión (solicitudes, inscripciones, edición del equipo); el anterior pasa a ser miembro normal.
- Si el capitán intenta abandonar el equipo siendo el único miembro, el equipo se disuelve. Si hay más miembros, debe delegar antes de poder salir.

---

## 4. Sistema del Logo

### Almacenamiento

El archivo nunca vive en el código ni en la base de datos. Solo se guarda la **URL pública** en el documento del equipo en MongoDB. En el MVP los logos se almacenan en **disco local del servidor**; la arquitectura está preparada para migrar a S3 o Cloudinary sin tocar la lógica de negocio.

### Subida

1. El capitán selecciona el archivo en el formulario del equipo.
2. El frontend envía el archivo al backend como `multipart/form-data`.
3. Spring Boot lo recibe, lo guarda en disco y devuelve la URL pública permanente.
4. Esa URL queda almacenada en MongoDB y se sirve como estático desde el servidor.

### Logo por defecto y moderación

- Si el equipo no tiene logo o el archivo falla, se muestra una imagen placeholder configurable.
- El admin puede **resetear el logo** de cualquier equipo desde su panel, volviendo al placeholder.
- **Restricciones:** solo `.jpg`, `.png` y `.webp`; máximo **2 MB**.

---

## 5. Sala de Torneo e Inscripción

### **A. Inscripción por equipo**

1. El capitán entra al detalle del torneo y presiona **"Inscribir equipo"**.
2. El backend valida que el equipo tenga el número mínimo de miembros requerido por el juego (5 Valorant, 4 Fortnite, 1 MLB).
3. La inscripción queda en `PENDING` hasta que el admin la apruebe.
4. El admin revisa y da `Aprobar` o `Rechazar`; al aprobar, el equipo aparece en la lista oficial de participantes.

### **B. Inscripción individual (MLB 1v1)**

- El jugador se inscribe directamente sin equipo desde la pantalla del torneo.
- El admin aprueba la entrada individualmente igual que en el flujo de equipo.

---

## 6. Match Day: Partidas y Resultados

Una vez generado el bracket, cada llave puede seguirse en el detalle del torneo.

1. **Apertura de apuestas:** El admin abre la ventana manualmente al iniciar la partida. Se cierra automáticamente a los 5 minutos o cuando el admin lo decida.
2. **Apuestas en vivo:** Los jugadores apuestan L-Coins a uno de los dos equipos. El tablero (pozo por bando y retorno implícito) se actualiza en **tiempo real** para todos los que tengan la página abierta.
3. **Registro del resultado:** Al terminar la partida el admin selecciona al ganador en el bracket. Opcionalmente carga las estadísticas del match (KDA, Home Runs, ERA, etc.).
4. **Liquidación:** El sistema distribuye el pozo entre los apostadores acertados de forma **automática e inmediata**.

---

## 7. Cierre de Torneo y Premios

Cuando se registra el resultado de la final:

1. El bracket queda como **completado**.
2. Se emiten los **trofeos** de posición (campeón, subcampeón, etc.) según el formato del bracket.
3. Si el torneo tenía tabla de L-Coins por posición configurada, el sistema distribuye los premios **automáticamente** a los jugadores correspondientes y lo registra en su historial como `TOURNAMENT_PLACEMENT_PRIZE`.
4. Todo esto es **idempotente**: aunque el proceso se ejecute más de una vez no se repite el pago.

---

## 8. Economía: el "Bank of LEON"

El historial económico de cada jugador es una tabla de transacciones con fecha, tipo y saldo resultante. Tipos posibles:

| Tipo | Concepto |
|------|----------|
| `WELCOME_BONUS` | Bono al registrarse por primera vez |
| `DAILY_CLAIM` | Reclamo diario de 100 LC |
| `BET_PLACED` | Descuento al colocar una apuesta |
| `BET_PAYOUT` | Pago al ganar en parimutuel |
| `BET_REFUND` | Devolución (si nadie apostó al bando ganador) |
| `TOURNAMENT_PLACEMENT_PRIZE` | Premio L-Coins por posición final en torneo |
| `ADMIN_ADJUSTMENT` | Ajuste manual por admin |

El backend valida el saldo de forma **atómica** antes de cualquier débito. No es posible quedar en negativo ni procesar la misma apuesta dos veces.

---

## 9. Notificaciones

El sistema guarda notificaciones en base de datos y las envía también por **WebSocket** si el usuario está conectado, sin necesitar que recargue la página.

- **Tipos implementados:** Notificaciones de plataforma para eventos relevantes al usuario (solicitudes, aprobaciones, etc.).
- **Interfaz:** Aparecen como **toasts** en pantalla y quedan disponibles para consulta posterior en `/api/me/notifications`.

---

## 10. Panel del Admin

El admin tiene acceso a funciones de gestión que un jugador normal no ve:

- **Equipos pendientes:** Aprobar, rechazar, suspender o resetear logo de cualquier equipo.
- **Torneos:** Crear, listar, cerrar/reabrir inscripciones, generar bracket, declarar ganador de cada llave, cargar estadísticas de partida y abrir/cerrar ventana de apuestas.
- **Entradas de torneo:** Aprobar o rechazar inscripciones individuales o de equipo.

---

## 11. Rankings (La "Pizarra de Fama")

Tablas públicas de los mejores jugadores por juego, basadas en los datos de la plataforma:

- **Filtro:** El usuario selecciona Valorant, Fortnite o MLB.
- **Datos:** Stats acumuladas de todas las partidas registradas en la plataforma.
- **Acceso:** Disponible sin necesidad de iniciar sesión.
