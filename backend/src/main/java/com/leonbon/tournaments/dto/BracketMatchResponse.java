package com.leonbon.tournaments.dto;

import com.leonbon.tournaments.BracketMatchStatus;
import com.leonbon.tournaments.BracketPool;

public record BracketMatchResponse(
        String id,
        String tournamentId,
        BracketPool bracketPool,
        int round,
        int indexInRound,
        String entryIdA,
        String entryIdB,
        String winnerEntryId,
        BracketMatchStatus status
) {}
