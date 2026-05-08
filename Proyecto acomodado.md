# Plataforma de Torneos: BON e-sports

## Módulo de Usuarios

### **Jugador**

- **Identidad:** Nombre de usuario único, número de jugador LEON (incremental y legible), nickname, correo electrónico opcional.

- **Perfil:** País, nombre real con control de visibilidad, redes sociales (Twitch, YouTube, X, Instagram, Discord).

- **Competencia:** Juego preferido, rango o división por juego (`VALORANT`, `FORTNITE`, `MLB`), historial de equipos y trofeos.

- **Economía:** Balance de **L-Coins** (bono de bienvenida: **5,000 LC**; reclamo diario: **100 LC**).

- **Estado:** `ACTIVE` o `SUSPENDED` (sanción admin).

---

### **Equipos**

- **General:** Nombre, tag (siglas), logo, región/servidor de actividad.

- **Liderazgo:** Capitán con poderes de gestión y coaches opcionales con rol diferenciado.

- **Roster:** Lista única de miembros (sin distinción titular/suplente).

- **Comercial:** Sponsors (líneas de texto libre) y URL de stream oficial del equipo.

- **Palmarés:** Trofeos y torneos ganados visibles en el perfil público del equipo.

- **Stats colectivas:** Stats de bracket (winrate, etc.) calculadas desde el historial de partidas.

---

## Gestión de Torneos

- **Configuración:** Nombre, organizadores, juego, formato del bracket.

- **Logística:** Ventanas de inscripción y competencia, tope de participantes aprobados.

- **Reglas:** Reglamento oficial y notas de elegibilidad (verificación manual por admin).

- **Premios:** Tabla de L-Coins por posición (cuántos puestos y cuánto por cada uno), notas de premio en texto libre.

- **Media:** Link al stream donde se transmite el torneo.

### **Motor de Brackets**

- **Formatos:** Eliminación simple, eliminación doble o Round Robin.

- **Generación:** El admin la ejecuta manualmente al cerrar inscripciones; el tamaño se ajusta a potencia de dos donde aplique.

- **Control:** El admin registra al ganador de cada llave y puede cargar estadísticas de esa partida; el bracket avanza solo.

---

## Estadísticas Especializadas

- **Ranking:** Tablas públicas por juego a través del módulo de plataforma.

- **Perfil público:** Snapshot cross-game del jugador: rango por juego, equipo actual, trofeos, redes.

- **Métricas por juego que el admin puede registrar:**

    - **Valorant:** KDA, Winrate, % de Headshots.

    - **Fortnite:** KD, Winrate, Victorias totales, Top 10, Modo más jugado.

    - **MLB The Show (1v1):**

        - _Bateo:_ AVG, Home Runs.
        - _Pitcheo:_ Innings lanzados, ERA, Carreras permitidas.

---

## Sistema de Apuestas (L-Coins)

- **Modelo:** Parimutuel dinámico (mismo principio que las apuestas en stream tipo Twitch).

- **Dinámica de cuotas:**

    - **Favorito** (más monedas apostadas) → retorno bajo.
    - **Underdog** (menos monedas apostadas) → retorno alto.

- **Control de ventana:**

    - **Apertura:** Manual por el admin al iniciar el partido.
    - **Cierre (lock):** Automático a los 5 minutos de haber abierto, o manual si el admin lo cierra antes.
    - **Interfaz:** Retorno implícito por moneda actualizado en tiempo real vía WebSocket.

- **Liquidación:** Automática en cuanto el admin confirma al ganador en el bracket; si nadie apostó al bando ganador, todos reciben reembolso.

---

## Stack tecnológico

- **Backend:** Spring Boot **3.5** (Java 17).

- **Base de datos:** MongoDB.

- **Frontend:** Vue.js 3 + Pinia + TypeScript + Tailwind CSS.

- **Tiempo real:** WebSocket/STOMP con SockJS (apuestas en vivo y notificaciones por usuario).

- **Archivos:** Disco local del servidor en este MVP; diseñado para migrar a S3/Cloudinary cambiando solo la capa de almacenamiento.
