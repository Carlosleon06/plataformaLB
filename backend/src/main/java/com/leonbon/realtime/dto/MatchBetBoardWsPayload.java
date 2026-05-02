package com.leonbon.realtime.dto;

import java.time.Instant;

/** Snap del tablero parimutuel enviado por WebSocket. */
public record MatchBetBoardWsPayload(
        String tournamentId,
        String matchId,
        long stakeOnEntryA,
        long stakeOnEntryB,
        Double impliedReturnPerCoinOnA,
        Double impliedReturnPerCoinOnB,
        Instant bettingClosesAt,
        int bettingWindowMinutes
) {}
