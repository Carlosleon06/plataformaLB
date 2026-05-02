package com.leonbon.bets.dto;

import com.leonbon.bets.BetStatus;
import java.time.Instant;

public record BetResponse(
        String id,
        String tournamentId,
        String matchId,
        String pickedEntryId,
        long amount,
        BetStatus status,
        Long payoutAmount,
        Instant createdAt,
        Instant resolvedAt
) {
}

