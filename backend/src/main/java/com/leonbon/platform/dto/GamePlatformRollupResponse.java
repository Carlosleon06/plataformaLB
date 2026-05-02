package com.leonbon.platform.dto;

import com.leonbon.tournaments.GameTitle;

public record GamePlatformRollupResponse(
        GameTitle game,
        int tournamentsEnteredApproved,
        int bracketMatchWins,
        int bracketMatchLosses,
        /** Winrate aproximada del bracket oficial (solo partidas marcadas COMPLETE). Null si sin partidas disputadas y cerradas. */
        Double bracketWinRatePctApprox,
        Integer valorantStatsSamples,
        Double avgValorantKda,
        Double avgValorantHeadshotPct,
        Integer fortniteStatsSamples,
        Double avgFortniteKillsPerMatch,
        Double avgFortniteKd,
        Double avgFortnitePlacement,
        Integer fortniteRoyaleVictoryMatches,
        Integer fortniteTop10Matches,
        String fortniteDominantModePlayed,
        Integer mlbStatsSamples,
        Double avgMlbBattingAvgGame,
        Double avgMlbHomeRunsGame,
        Double avgMlbInningsPitchedGame,
        Double avgMlbEraGame,
        Double avgMlbRunsAllowedGame
) {}
