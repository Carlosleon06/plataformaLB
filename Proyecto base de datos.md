# Proyecto base de datos: BON e-sports

La base de datos es **MongoDB**. El diseño original contemplaba 6 módulos conceptuales; al construir el sistema esos módulos se tradujeron en **13 colecciones** para separar responsabilidades y mantener las consultas simples.

---

## Colecciones

### `users`
Un documento por cuenta registrada.

- `_id` — ObjectId de Mongo.
- `leonPlayerNumber` — Número entero incremental y legible (ej. 10001). Se asigna al registrarse usando la colección `numeric_sequences`.
- `username` — Único. El nombre con el que entra al sistema.
- `emailNormalized` — Único (sparse; puede ser nulo si no se proporcionó).
- `passwordHash` — Hash BCrypt.
- `nickname` — Nombre público en pantalla.
- `fullName` — Nombre real opcional.
- `profileShowFullName` — Booleano: si el jugador quiere que se muestre su nombre real.
- `country` — País/residencia opcional.
- `twitchProfileUrl`, `youtubeChannelUrl`, `xProfileUrl`, `instagramProfileUrl`, `discordHandle` — Redes sociales opcionales.
- `preferredGame` — Juego principal (`VALORANT`, `FORTNITE`, `MLB`).
- `rankLabelByGame` — Mapa `{ "VALORANT": "Diamante", "MLB": "..." }` con el rango textual por juego.
- `status` — `ACTIVE` o `SUSPENDED`.
- `role` — `PLAYER` o `ADMIN`.
- `leonCoinsBalance` — Saldo actual de L-Coins (entero largo).
- `lastDailyClaimAt` — Timestamp del último claim diario; sirve para validar si ya reclamó en la ventana actual.
- `createdAt`, `updatedAt`

---

### `teams`
Un documento por equipo.

- `_id`
- `name` — Nombre del equipo.
- `tag` — Siglas cortas (ej. "LBN").
- `regionServer` — Servidor o región de actividad.
- `logoUrl` — URL pública de la imagen almacenada en disco (o placeholder si no hay logo).
- `status` — `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`.
- `captainUserId` — ID del usuario con rol de capitán.
- `coachUserIds` — Lista de IDs de usuarios con rol de coach.
- `memberUserIds` — Lista de todos los miembros del equipo.
- `sponsorLines` — Lista de textos cortos (patrocinadores / partners).
- `canonicalStreamUrl` — URL de stream oficial del equipo.
- `createdAt`, `updatedAt`

---

### `team_join_requests`
Una por cada solicitud de ingreso a un equipo.

- `_id`
- `teamId` — Equipo al que se solicita.
- `requesterUserId` — Usuario que pide unirse.
- `status` — `PENDING`, `ACCEPTED`, `REJECTED`.
- `createdAt`, `updatedAt`

---

### `tournaments`
Un documento por torneo.

- `_id`
- `name`
- `organizers` — Nombre de los organizadores (texto).
- `game` — `VALORANT`, `FORTNITE` o `MLB`.
- `format` — `SINGLE_ELIM`, `DOUBLE_ELIM` o `ROUND_ROBIN`.
- `lifecycleStatus` — Estado general: `REGISTRATION_OPEN`, `REGISTRATION_CLOSED`, `COMPLETED`, etc.
- `registrationStartAt`, `registrationEndAt` — Ventana de inscripciones.
- `competitionStartAt`, `competitionEndAt` — Ventana de competencia.
- `streamUrl` — Link al stream del evento.
- `rulesHtml` — Reglamento público (texto libre).
- `eligibilityNotes` — Requisitos de elegibilidad (verificación manual por admin).
- `prizeNotes` — Descripción del premio en texto libre.
- `maxApprovedParticipants` — Tope de participantes aprobados (null = sin límite).
- `bracketSize` — Potencia de dos calculada al generar el bracket; null antes de generarlo.
- `prizeWinnerSlots` — Cuántos puestos reciben L-Coins al cierre del torneo.
- `prizeLeonCoinsByPlacement` — Lista ordenada de L-Coins por puesto (índice 0 = campeón).
- `placementPrizeLedgerCompletedAt` — Timestamp que indica que los premios ya se repartieron (mecanismo de idempotencia).
- `createdAt`, `updatedAt`

---

### `tournament_entries`
Una por cada inscripción (equipo o jugador individual) a un torneo.

- `_id`
- `tournamentId`
- `type` — `TEAM` o `PLAYER`.
- `teamId` — Relleno en entradas de tipo `TEAM`.
- `playerId` — Relleno en entradas individuales `PLAYER` (MLB 1v1).
- `status` — `PENDING`, `APPROVED`, `REJECTED`.
- `selectedRosterUserIds` — IDs de los miembros que participan en este torneo específico.
- `createdAt`, `updatedAt`

---

### `bracket_matches`
Una por cada llave/partida generada en el bracket.

- `_id`
- `tournamentId`
- `round` — Número de ronda (1 = primera ronda).
- `indexInRound` — Posición dentro de la ronda.
- `bracketPool` — Sub-bracket (`WB` = winners bracket, `LB` = losers bracket en doble eliminación).
- `entryIdA`, `entryIdB` — IDs de las entradas enfrentadas. `null` indica BYE.
- `winnerEntryId` — ID de la entrada ganadora; `null` hasta que el admin lo declare.
- `status` — `WAITING`, `READY`, `COMPLETE`.
- `scheduledStartAt` — Hora sugerida del partido (referencia de agenda).
- `bettingWindowClosesAt` — Timestamp hasta el que las apuestas están abiertas. `null` si la ventana está cerrada.
- `createdAt`, `updatedAt`

---

### `bracket_match_stats`
Una por partido (1:1 con `bracket_matches`). Solo existe si el admin cargó estadísticas.

- `_id`
- `matchId` — Único.
- `tournamentId`
- `game` — Juego del torneo.
- `recordedByAdminUserId` — Quién guardó la última revisión.
- `revision` — Contador de veces que se ha editado.
- `recordedAt`
- `valorantRows` — Lista de filas por jugador con: `userId`, `kda`, `kills`, `deaths`, `assists`, `headshotPct`.
- `fortniteRows` — Lista de filas por jugador con: `userId`, `kills`, `deaths`, `placement`, `modePlayed`.
- `mlbRows` — Lista de filas por jugador con: `userId`, `battingAvgGame`, `homeRunsGame`, `inningsPitchedGame`, `eraGame`, `runsAllowedGame`.

---

### `bets`
Una por apuesta colocada. Índice único en `(userId, matchId)` para garantizar una apuesta por usuario por partido.

- `_id`
- `userId`
- `tournamentId`
- `matchId`
- `pickedEntryId` — ID de la entrada por la que apostó.
- `amount` — L-Coins apostados.
- `status` — `PENDING`, `WON`, `LOST`, `REFUNDED`.
- `payoutAmount` — L-Coins recibidos al resolverse (null si aún no se resuelve).
- `createdAt`, `resolvedAt`

---

### `transactions`
Una por cada movimiento de L-Coins. Historial económico completo del usuario.

- `_id`
- `userId`
- `type` — `WELCOME_BONUS`, `DAILY_CLAIM`, `BET_PLACED`, `BET_PAYOUT`, `BET_REFUND`, `TOURNAMENT_PLACEMENT_PRIZE`, `ADMIN_ADJUSTMENT`.
- `amount` — Positivo = ingreso; negativo = gasto.
- `balanceAfter` — Saldo resultante tras el movimiento.
- `ref` — Referencia opcional al documento relacionado (ej. `"bet:abc123"`).
- `createdAt`

---

### `trophy_awards`
Una por cada posición otorgada al cerrar un torneo.

- `_id`
- `tournamentId`, `tournamentName`
- `game`, `tournamentFormat`
- `placement` — 1 = campeón, 2 = subcampeón, etc.
- `badgeLabel` — Texto del trofeo (ej. "Campeón").
- `tournamentEntryId` — Entrada que obtuvo esa posición.
- `entryType` — `TEAM` o `PLAYER`.
- `teamId` — Si es entrada de equipo.
- `playerId` — Si es entrada individual.
- `creditedMemberUserIds` — Lista de los jugadores del roster que reciben el crédito.
- `awardedAt`

---

### `user_notifications`
Una por cada notificación generada para un usuario.

- `_id`
- `userId`
- `category` — `TEAM_JOIN_ACCEPTED`, `TEAM_JOIN_REJECTED`, `TOURNAMENT_ENTRY_APPROVED`, `TOURNAMENT_ENTRY_REJECTED`.
- `title` — Título corto.
- `summary` — Texto para mostrar en toast.
- `teamIdRef`, `tournamentIdRef`, `tournamentEntryIdRef` — Referencias opcionales al recurso relacionado.
- `createdAt`

---

### `numeric_sequences`
Contador incremental para IDs legibles. Solo tiene un documento activo:

- `_id` = `"user_leon_player_number"`
- `latest` — Último número asignado. Se incrementa con cada nuevo registro.

---

## Relaciones entre colecciones

```
users ──────────────────────── teams (captainUserId, memberUserIds, coachUserIds)
users ──────────────────────── team_join_requests (requesterUserId)
users ──────────────────────── bets (userId)
users ──────────────────────── transactions (userId)
users ──────────────────────── user_notifications (userId)
users ──────────────────────── trophy_awards (creditedMemberUserIds, playerId)

teams ──────────────────────── tournament_entries (teamId)
teams ──────────────────────── team_join_requests (teamId)
teams ──────────────────────── trophy_awards (teamId)

tournaments ────────────────── tournament_entries (tournamentId)
tournaments ────────────────── bracket_matches (tournamentId)
tournaments ────────────────── trophy_awards (tournamentId)

tournament_entries ─────────── bracket_matches (entryIdA, entryIdB, winnerEntryId)
tournament_entries ─────────── bets (pickedEntryId)
tournament_entries ─────────── trophy_awards (tournamentEntryId)

bracket_matches ────────────── bracket_match_stats (matchId)
bracket_matches ────────────── bets (matchId)
```

> En MongoDB no hay foreign keys. Todas estas relaciones son **referencias por ID** que el backend resuelve en tiempo de ejecución.
