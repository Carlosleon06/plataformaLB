package com.leonbon.tournaments.dto;

import com.leonbon.tournaments.BracketMatchStatus;
import com.leonbon.tournaments.BracketPool;
import java.time.Instant;

public record BracketMatchResponse(
        String id,
        String tournamentId,
        BracketPool bracketPool,
        int round,
        int indexInRound,
        String entryIdA,
        String entryIdB,
        String winnerEntryId,
        BracketMatchStatus status,
        /** Hora escalonada sugerida para el orden de partidas. */
        Instant scheduledStartAt,
        /** Duración máx/config de ventana cuando el admin la abre (minutos hasta auto-cierre). */
        int bettingWindowMinutes,
        /** Cierra cuando el tiempo llega aquí si el admin abrió apuestas. */
        Instant bettingClosesAt,
        /** Moneda apostada ({@link #entryIdA}). */
        long totalStakeEntryA,
        long totalStakeEntryB,
        /** Cuota estimada tipo Twitch: pozo_total / lado (sin contar apuesta incremental). Null si no hay volumen en ese lado. */
        Double impliedReturnPerCoinOnA,
        Double impliedReturnPerCoinOnB
) {}
