### 1. Dashboard de Inicio (The Hub)

Es la central de inteligencia del usuario.

- **Header (Persistente):**
    
    - **Izquierda:** Logo LEON BON + Buscador global (Jugadores/Equipos).
        
    - **Centro:** Balance de L-Coins.
        
    - **Derecha:** Botón "Daily Claim" (Brilla si está disponible) + Avatar que despliega menú (Perfil, Ajustes, Logout).
        
- **Sección de Rankings (Top 3):** Tres tarjetas destacadas con los reyes de **Valorant**, **Fortnite** y **MLB**.
    
- **Carrusel Competitivo:** Banners con los torneos "Inscribiendo" o "En vivo".
    
- **Feed Social:** Notificaciones globales tipo: _"¡El equipo [TEAM_NAME] se ha coronado campeón del Open de Verano!"_.

### 2. Perfil del Jugador (Seccionado)

Diseñado para que solo se vea lo que realmente importa.

- **Hero Section:** * Visual de rango de su juego principal(Platino, Diamante, etc.).
    
    - Tag de equipo: Si no tiene, aparece como **"Free Agent"** (Agente libre).
        
- **Navegación de Stats (Tabs Dinámicas):**
    
    - Al dar clic en la pestaña de un juego en los que tiene actividad, si no tiene actividad en el juego no se muestra, se despliega el resumen de métricas que definimos (KDA, HR, etc.).
        
- **Timeline de Carrera:** Un historial vertical que muestra: "Se unió a X", "Ganó Torneo Y", "Salió de Z".

### 3. Ciclo de Vida del Equipo (Lógica de Gestión)

Este es el flujo técnico que programaremos en Java:

#### **A. Creación y Validación**

1. **Jugador A** rellena el formulario (Nombre, Logo, Siglas).
    
2. El sistema lo marca como `Estado: Pendiente` y le asigna el rol de **Capitán**.
    
3. El **Admin** recibe una notificación, revisa que todo esté en orden y le da a `Aprobar`.
    
4. El equipo aparece en el buscador y el perfil del Jugador A se actualiza.
    

#### **B. Reclutamiento (Solicitudes)**

1. **Jugador B** busca el equipo y presiona el botón **"Solicitar Unirse"**.
    
2. El **Capitán** recibe la solicitud en su panel de gestión de equipo.
    
3. El Capitán revisa el perfil/stats del Jugador B y decide: `Aceptar` o `Rechazar`.
    

#### **C. Sucesión de Mando (Traspaso de Capitanía)**

- El Capitán actual tiene una opción: **"Delegar Capitanía"**.
    
- Selecciona a un miembro del roster.
    
- **Trigger de Java:** El sistema intercambia los roles en la base de datos. El antiguo capitán pasa a ser jugador normal y el nuevo adquiere los permisos de gestión (aprobar miembros, inscribir a torneos).
    
- _Regla:_ Si el capitán intenta abandonar el equipo y es el único miembro, el equipo se disuelve (o se le obliga a delegar si hay más gente).


## Sistema del logo
### 1. El Almacenamiento (Dónde vive la imagen)

No guardaremos el archivo en el código ni en la base de datos. Usaremos un enfoque de **"Punteros de URL"**:

- **Servidor de Archivos:** Usaremos un servicio como **Cloudinary**, **AWS S3** o incluso una carpeta local en el servidor (si el proyecto es pequeño).
    
- **En MongoDB:** Guardaremos únicamente el **String con la URL** (ej: `https://res.cloudinary.com/leonbon/logos/equipo_123.png`).
    

### 2. Flujo de Subida (Upload Flow)

Cuando el jugador crea el equipo:

1. En **Vue.js**, el usuario selecciona el archivo.
    
2. El frontend hace una petición `POST` al backend de **Spring Boot** enviando el archivo como un `MultipartFile`.
    
3. **Java** recibe la imagen, la procesa (opcionalmente le cambia el tamaño para que todos los logos sean, por ejemplo, de 512x512 px) y la sube al servidor de archivos.
    
4. Una vez subida, el servidor de archivos le devuelve a Java la **URL pública**.
    
5. Java guarda esa URL en el documento del equipo en **MongoDB**.
    

### 3. Logo por Defecto

Para evitar que la página se vea vacía o con errores:

- Si el equipo está recién creado o el Admin aún no lo aprueba, el sistema asigna una **URL de imagen por defecto** (un escudo genérico o la letra inicial del equipo).
    
- En **Vue**, usaremos una directiva para que, si la carga del logo falla, se muestre automáticamente el logo de LEON BON.
    
### 4. Seguridad y Validación

Como el Admin tiene que aprobar el equipo, la lógica del logo incluye:

- **Restricción de Formato:** Solo se aceptan `.jpg`, `.png` y `.webp`.
    
- **Restricción de Peso:** Máximo 2MB para no saturar el servidor.
    
- **Moderación:** Si un equipo sube un logo ofensivo, el Admin puede "Resetear Logo" desde su panel, devolviéndolo a la imagen por defecto y notificando al Capitán.


## 3. Sala de Torneo e Inscripción (Flujo del Capitán)

Este flujo ocurre cuando un torneo está en estado `Reclutando`.

### **A. La Decisión del Capitán**

1. El Capitán entra a la **Sala del Torneo**.
    
2. El sistema detecta su rol y habilita el botón **"Inscribir Equipo"**.
    
3. **Validación Automática (Java):** Antes de dejarlo dar clic, Spring Boot revisa:
    
    - ¿El equipo tiene el número mínimo de jugadores requeridos? (Ej. 5 para Valorant).
        
    - ¿Los rangos de los jugadores cumplen con el requisito del torneo?
        
    - ¿El equipo ya está inscrito en otro torneo que se juega a la misma hora?

### **B. Selección del Roster para el Torneo**

Como en tu esquema el roster es una lista única (sin suplentes fijos), el Capitán debe confirmar quiénes jugarán:

1. Se abre un modal con la lista de todos los miembros del equipo.
    
2. El Capitán selecciona a los participantes (ej. los 5 que jugarán este torneo específico).
    
3. Al confirmar, el estado del equipo en ese torneo pasa a `Pendiente de Aprobación`.
4. Mientras un equipo esta registrado a un torneo o al menos esta por validarse si el admin deja que participe o no, el equipo no puede disolverse y ningun miembro que se haya indicado que participara en el torneo se puede salir del equipo.
    
### **C. Verificación del Admin**

1. El **Admin** recibe la solicitud en su panel.
    
2. Revisa rápidamente los perfiles (para evitar _smurfs_ o cuentas sospechosas).
    
3. Al dar `Aprobar`, el equipo aparece oficialmente en la **Lista de Participantes** y se le resta el cupo al torneo (ej. 15/16 equipos).
    

---

## 4. El "Match Day" (Flujo de Juego y Resultados)

Una vez que las inscripciones se cierran y el **Motor de Brackets** genera los cruces automáticamente:

1. **Notificación de Partida:** El sistema envía una alerta al Capitán: _"Tu partida contra [Equipo Rival] empieza en 15 minutos"_.
    
2. **Acceso a la "Arena":** Se habilita un link a la **Sala de Partida** (donde está el stream y las apuestas).
    
3. **Presencia del Admin:** * El Admin se une a la partida en el juego (Valorant, MLB, etc.) como espectador.
    
    - El Admin marca en la plataforma un botón de **"Partida en curso"**.
        
    - **Trigger Automático:** En ese instante se abren las apuestas de Leon Coins por 5 minutos.
        

---

### 5. Cierre y Reparto de Premios (Flujo Final)

1. **Fin de la Partida:** El Admin ve la pantalla de victoria/derrota en el juego.
    
2. **Carga de Datos:** En su panel, el Admin selecciona al equipo ganador y rellena los campos de estadísticas que definimos (KDA, Home Runs, etc.).
    
3. **Ejecución de Java (El momento de la verdad):**
    
    - El ganador avanza en el bracket.
        
    - Se actualizan las estadísticas individuales de cada jugador en sus perfiles.
        
    - Se reparten las Leon Coins a los apostadores ganadores.
        
4. **Final del Torneo:** Si es la final, el sistema detecta que el bracket terminó:
    
    - Se otorga el **Premio Monetario** (si aplica) al balance del equipo/capitán.
        
    - Se añade el trofeo al historial del equipo.


## Flujo MLB The Show 2026 (Individual)

#### **1. Inscripción Directa**

- **Sin Capitán:** En la Sala del Torneo de MLB, el botón de "Inscribir Equipo" cambia por **"Participar Individualmente"**.
    
- **Validación de Rango:** Java verifica el rango de pitcheo/bateo del jugador en su perfil antes de dejarlo entrar.
    
- **Confirmación:** Al ser 1v1, el Admin aprueba directamente al jugador. No hay selección de roster porque el jugador es su propio equipo.
    

#### **2. Dinámica de la Partida (El "Oráculo")**

- **Admin Espectador:** El Admin se une a la partida o supervisa el stream del enfrentamiento.
    
- **Control de Apuestas:** Al ser un juego con mucha estadística (innings, carreras), las apuestas de Leon Coins se abren igual al inicio, pero el multiplicador suele ser más volátil porque depende de un solo individuo, no de un grupo.
    

#### **3. Carga de Estadísticas de "Doble Rol"**

Aquí es donde Java hace el trabajo pesado. Al terminar la partida, el Admin tiene un formulario especial para MLB con dos pestañas:

- **Pestaña Bateador:** El Admin registra el `AVG` del partido y los `Home Runs`.
    
- **Pestaña Pitcher:** El Admin registra los `Innings Lanzados`, `ERA` del encuentro y `Carreras Permitidas`.
    
- **Actualización:** Java toma estos datos y los **promedia** con los que el jugador ya tenía en su perfil para actualizar su Ranking Global.




## 6. Flujo de Economía: El "Bank of LEON"

Ya definimos el bono de 5,000 y el reclamo de 100 diarios, pero falta el historial. Un usuario que apuesta quiere saber en qué ganó y en qué perdió.

- **Historial de Transacciones:** Una sección dentro del perfil donde el jugador ve una tabla con:
    
    - **Fecha/Hora.**
        
    - **Concepto:** "Bono Diario", "Apuesta Ganada (Match #123)", "Apuesta Perdida".
        
    - **Monto:** $+100$, $-500$, etc.
        
- **Seguridad de Fondos:** Java debe validar que el saldo sea suficiente _antes_ de confirmar la apuesta. Si el usuario hace clic 10 veces rápido, Java debe bloquear el balance para que no se quede en negativo.
    

## 7. Flujo de Notificaciones (Alertas en Tiempo Real)

Sin esto, el usuario tiene que estar refrescando la página para saber si le aceptaron en un equipo o si ya empezó su partida.

- **Tipos de Notificaciones:**
    
    - **Informativas:** "Tu solicitud para unirte al equipo [X] ha sido aceptada".
        
    - **Acción:** "Eres el nuevo Capitán del equipo".
        
    - **Urgentes:** "Tu partida de Torneo comienza en 5 minutos. ¡Entra a la Arena!".
        
- **Implementación:** Usaremos los **WebSockets** que mencionamos. Java envía el mensaje y en Vue aparece un "Toast" (una burbujita de notificación) en la esquina superior derecha.
    

## 8. El Panel Global del Admin (Master Control)

Hasta ahora hablamos del Admin en el torneo, pero falta su flujo de "mantenimiento" de la plataforma.

- **Gestión de Usuarios:** Posibilidad de **Banear/Castigar** a un usuario (cambiar su estado de "Activo" a "Sancionado") si el Admin detecta toxicidad en el stream o trampas.
    
- **Gestión de Rankings:** Un botón para "Refrescar Rankings" si hay algún error en los cálculos de promedios de MLB o Valorant.
    
- **Aprobaciones Pendientes:** Una "Bandeja de Entrada" única donde el Admin ve:
    
    1. Equipos nuevos creados (para aprobar logo y nombre).
        
    2. Reportes de jugadores (si decides poner un botón de reporte).
        

## 9. Vista de Ranking Global (La "Pizarra de Fama")

Este flujo es vital para la competitividad.

- **Filtros por Juego:** El usuario selecciona "Valorant", "Fortnite" o "MLB".
    
- **Lógica de Desempate (Java):** Si dos jugadores tienen el mismo Winrate en Valorant, Java debe decidir quién va primero (ej. el que tenga más Headshots %).
    