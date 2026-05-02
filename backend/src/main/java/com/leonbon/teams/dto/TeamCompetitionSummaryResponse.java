package com.leonbon.teams.dto;

public record TeamCompetitionSummaryResponse(
        int tournamentsWithApprovedEntry,
        int bracketWins,
        int bracketLosses,
        Double winRatePct
) {}
