package com.leonbon.tournaments.dto;

import com.leonbon.tournaments.BracketMatchStatus;

public record BracketMatchResponse(
        String id,
        String tournamentId,
        int round,
        int indexInRound,
        String entryIdA,
        String entryIdB,
        String winnerEntryId,
        BracketMatchStatus status
) {}
