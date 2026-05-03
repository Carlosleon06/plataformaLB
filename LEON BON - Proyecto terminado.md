# LEON BON — Plataforma de torneos

## Qué es

**LEON BON** es una plataforma web de torneos competitivos para tres juegos: **Valorant**, **Fortnite** y **MLB The Show** (1v1). No es un juego en sí, sino el **lobby** donde se organiza todo lo que rodea a la competencia: se crean equipos, se inscriben en torneos, se sigue el bracket en tiempo real, y existe una moneda interna llamada **L-Coins** con la que los jugadores pueden apostar en partidas, recibir premios por su colocación final o simplemente acumular racha de presencia a través de un bono diario.

La idea central es que el jugador tenga **un solo lugar** donde ver en qué torneo está, cómo le va al bracket, cuánto lleva apostado y qué tan arriba está en el ranking de su juego. El admin, por su parte, tiene control total sobre el ciclo de vida del evento sin necesidad de tocar base de datos.

---

## Quién puede usarla

La plataforma distingue dos tipos de usuario:

- **Jugador (PLAYER):** Se registra, crea o se une a equipos, se inscribe en torneos, apuesta en partidas del bracket y consulta su historial económico y trofeos.

- **Administrador (ADMIN):** Además de todo lo anterior, aprueba o rechaza equipos, crea torneos, genera brackets, registra resultados, abre y cierra ventanas de apuestas, y puede cargar estadísticas de una partida terminada.

La distinción entre roles es automática: un conjunto de usernames definido en configuración recibe privilegios de admin al registrarse. Todo lo demás es jugador por defecto.

---

## Módulo de usuarios

Cada cuenta tiene un **nombre de usuario único**, un **nickname** (el que se muestra en público), **email opcional**, y campos de perfil que el propio jugador llena: país, redes sociales (Twitch, YouTube, X, Instagram, Discord), juego preferido y su **rango o división** en cada título donde compite.

Además, el sistema asigna automáticamente un **número de jugador LEON** único y legible para identificar a cada persona sin exponer IDs internos.

El estado de una cuenta puede ser **activo** o **castigado**, lo que le permite al admin suspender la actividad de alguien sin eliminar su historial.

---

## Módulo de equipos

Un equipo tiene nombre, **tag** (siglas), región o servidor de actividad, logo y una lista de miembros. Dentro del equipo existe un **capitán** que tiene poderes de gestión, y opcionalmente **coaches** con un rol diferenciado.

Crear un equipo no lo publica de inmediato. Queda en estado **pendiente** hasta que un admin lo revisa y lo **aprueba**. Este filtro sirve para moderar logos y nombres antes de que aparezcan públicamente en la plataforma.

Una vez aprobado, el equipo es visible en el listado. Otros jugadores pueden **solicitar unirse**; el capitán recibe esas solicitudes y decide aceptar o rechazar. El capitán también puede **delegar la capitanía** a otro miembro, editar la información comercial del equipo (sponsors, URL de stream) y subir o cambiar el logo.

El equipo acumula un historial de torneos y **trofeos** que quedan visibles en su perfil público.

---

## Módulo de torneos

Un torneo puede ser de eliminación simple, eliminación doble o round-robin, y puede definirse para cualquiera de los tres juegos soportados. El admin configura fechas de inscripción y de competencia, un texto de reglas, notas de elegibilidad, link de stream donde se transmite, y opcionalmente una **tabla de premios en L-Coins** por posición final (por ejemplo: 1er lugar 5000 LC, 2do lugar 2000 LC, 3er lugar 1000 LC).

La inscripción puede ser **por equipo** —el capitán inscribe al roster siempre que el equipo tenga el mínimo de miembros que exige el juego— o **individual**, útil para el formato 1v1 de MLB. Las inscripciones también pasan por aprobación del admin antes de entrar al bracket.

Una vez cerradas las inscripciones, el admin **genera el bracket** y los partidos quedan visibles para todos. A partir de ahí gestiona los resultados llave por llave: marca al ganador, el bracket avanza automáticamente y, si el torneo tenía tabla de premios, los L-Coins se distribuyen al completarse la competencia.

---

## Economía: L-Coins

Las **L-Coins** son la moneda interna. No tienen valor fuera de la plataforma; sirven para apostar, acumular y competir por el top del historial.

Cada movimiento queda registrado en un historial personal con fecha, tipo y saldo después de la operación. Los tipos de movimiento que existen son:

- **Bono de bienvenida** — Se acreditan al registrarse por primera vez (por defecto **5,000 LC**).
- **Reclamo diario** — Cada día de plataforma el jugador puede reclamar **100 LC** desde su inicio. La ventana se resetea en un horario fijo, no cada 24 h relativas.
- **Apuesta colocada** — Descuento al apostar en una partida.
- **Pago de apuesta** — Abono al ganar en el sistema parimutuel.
- **Reembolso de apuesta** — Devolución en casos especiales (si nadie apostó al bando ganador, se devuelve a todos).
- **Premio por colocación** — L-Coins del torneo distribuidos automáticamente al cerrarse según la tabla configurada por el admin.

El historial económico se ve en la pantalla de **inicio**, junto al saldo actual y el botón de reclamo diario.

---

## Apuestas parimutuel

En cada partida del bracket, el admin puede **abrir una ventana de apuestas** durante la cual los jugadores eligen uno de los dos equipos y colocan la cantidad de L-Coins que quieran arriesgar. La ventana tiene un **cierre automático** configurable (por defecto 5 minutos desde que se abre), aunque el admin también puede cerrarla manualmente.

El modelo es **parimutuel**: no hay cuotas fijas. El pozo es la suma de todas las apuestas colocadas, y se reparte entre los que acertaron en proporción a cuánto apostaron. Quien apuesta al favorito (el bando donde está el grueso del dinero) gana menos; quien apuesta al underdog gana más si acierta.

El tablero de apuestas de cada partida **se actualiza en tiempo real** mediante WebSocket: cuando alguien coloca una apuesta, todos los que tienen la página abierta ven el pozo moverse y la cuota implícita cambiar sin recargar.

Al declarar un ganador en el bracket, el sistema **liquida automáticamente** todas las apuestas de esa partida.

---

## Pantallas principales (experiencia de usuario)

**Inicio / Hub** — La primera pantalla al entrar. Si el usuario tiene sesión, muestra su resumen de perfil (número de jugador, usuario, nickname, estado, rol y saldo de L-Coins), el botón de **reclamo diario**, y la tabla de **historial económico**. Si es admin, aquí tiene accesos directos a sus herramientas. El chip **LC** en el header también lleva a esta pantalla desde cualquier parte de la app.

**Torneos** — Listado de todos los torneos publicados. Desde ahí se entra al detalle de cada uno, donde se ven los equipos inscritos, el bracket con sus partidos y la UI de apuestas en tiempo real en cada llave que lo permita.

**Equipos** — Catálogo de equipos aprobados con foto, tag y región. Desde el detalle de cada equipo se puede ver el roster, el palmarés de trofeos y la información comercial. Si eres capitán de ese equipo, ves además la gestión de solicitudes, logos y datos del equipo.

**Rankings** — Tablas de clasificación por juego basadas en los datos de la plataforma: Valorant, Fortnite y MLB.

**Perfil público** — Ficha de cualquier jugador: número LEON, nickname, juego preferido, rango por juego, equipo actual, trofeos y redes publicadas.

**Perfil propio / Editar** — Formulario para completar o actualizar redes, país, rangos y preferencias de visibilidad.

**Admin: equipos pendientes** — Cola de equipos esperando revisión con acciones de aprobación, rechazo, suspensión y reset de logo.

**Admin: torneos** — Vista de gestión donde se crean eventos y, dentro de cada uno, se controla el ciclo completo: inscripciones, bracket, partidos, apuestas y resultados.

---

## Tiempo real y notificaciones

La plataforma mantiene una **conexión WebSocket** abierta mientras el usuario navega. A través de ella llegan dos tipos de mensajes:

- **Tablero de apuestas** por partida: cualquiera que vea una página de torneo recibe actualizaciones del pozo en vivo.
- **Notificaciones personales**: eventos relevantes para el usuario (por ejemplo aprobación de equipo, solicitud recibida, etc.) que aparecen como **toasts** en pantalla y quedan guardados para consulta posterior.

---

## Stack

- **Backend:** Java 17 con Spring Boot 3.5.
- **Base de datos:** MongoDB.
- **Frontend:** Vue 3, TypeScript, Pinia, Vue Router, Tailwind CSS, Vite.
- **Tiempo real:** STOMP sobre WebSocket/SockJS.
- **Archivos (logos):** almacenados en disco local del servidor en este MVP.

---

*Este documento describe la plataforma en su estado terminado. Para el flujo original de visión del producto ver `Flujo del sistema.md` y `Proyecto acomodado.md`.*
