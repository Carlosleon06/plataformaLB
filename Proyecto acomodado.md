# Plataforma de Torneos: LEON BON

## Módulo de Usuarios

### **Jugador**

- **Identidad:** Nombre real, Correo electrónico, ID numérico, Nickname.
    
- **Perfil:** País, Redes sociales y Plataformas de stream.
    
- **Competencia:** Juego principal, Rango por juego, Juegos activos, Historial de equipos.
    
- **Economía:** Balance de **Leon Coins** (Bono inicial: **5,000 L-Coins**). Los jugadores recibirán 100 L coins diarios.
    
- **Estado:** Activo o Castigado (Sanciones).
    

### **Equipos**

- **General:** Nombre del equipo, Logo, Servidor/Región de actividad.
    
- **Liderazgo:** Capitán y Coach (Jugador con rol asignado).
    
- **Roster:** Lista única de jugadores (sin distinción de titulares/suplentes).
    
- **Comercial:** Sponsors del equipo y Plataformas de stream asociadas.
    
- **Palmarés:** Historial de torneos y Trofeos ganados.
    
- **Stats Colectivas:** Winrate y KDA general del equipo.
    

---

## Gestión de Torneos

- **Configuración:** Organizadores, Juego, Formato del evento.
    
- **Logística:** Fechas de inscripción, Fecha de competencia, Número de participantes.
    
- **Reglas:** Reglamento oficial y Requisitos de elegibilidad (Verificación manual por Admin).
    
- **Premios:** Bolsa económica distribuida por posición.
    
- **Media:** Link a la plataforma de Stream donde se transmite el torneo.
    

### **Motor de Brackets**

- **Formatos:** Eliminación simple, doble o Round Robin.
    
- **Generación:** Automática al cierre de inscripciones.
    
- **Control:** Registro manual de resultados por el Admin (Selección rápida entre los dos enfrentados).
    

---

## Estadísticas Especializadas

- **Global:** Ranking Top 10 por juego.
    
- **Visualización:** Seccionado por juego en el perfil del jugador.
    
- **Métricas por Juego:**
    
    - **Valorant:** KDA, Winrate, % de Headshots.
        
    - **Fortnite:** KD, Winrate, Victorias totales, Top 10, Modo más jugado.
        
    - **MLB The Show 2026 (1v1):**
        
        - _Bateo:_ AVG, Home Runs.
            
        - _Pitcheo:_ Innings lanzados, ERA, Carreras permitidas.
            

---

## Sistema de Apuestas (LEON Coins)

- **Modelo:** Formato Twitch (Parimutuel dinámico).
    
- **Dinámica de Cuotas:**
    
    - **Favorito:** (Más monedas apostadas) → Multiplicador bajo.
        
    - **Underdog:** (Menos monedas apostadas) → Multiplicador alto.
        
- **Automatización de Partida:**
    
    - **Apertura:** Automática al llegar la hora programada del match.
        
    - **Cierre (Lock):** Automático a los 5 minutos de haber iniciado.
        
    - **Interfaz:** Visualización del multiplicador y ganancia estimada en tiempo real.
        
- **Liquidación:** El pago se ejecuta automáticamente una vez que el Admin confirma al ganador en el bracket.

### stack tecnológico

- **Backend:** Spring Boot 3.x (Java).
    
- **Base de Datos:** MongoDB.
    
- **Frontend:** **Vue.js 3** (con Pinia para manejar las Leon Coins).
    
- **Estilo:** **Tailwind CSS** (funciona igual de bien con Vue).
    
- **Tiempo Real:** **Pusher** o **WebSockets** (para que las apuestas se actualicen en vivo).
