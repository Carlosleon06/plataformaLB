package com.leonbon.teams.dto;

/** Métricas desde {@code BracketMatchStats} del roster en partidos atribuidos al equipo (entradas APPROVED). */

public record TeamCollectiveBracketStatsResponse(
        int approvedTournaments,
        int attributedCompletedMatchesWithStats,
        TeamValorantAgg valorant,
        TeamFortniteAgg fortnite,
        TeamMlbAgg mlb

) {

    public record TeamValorantAgg(int playerRows, Double avgKda, long kills, long deaths, long assists, Double avgHsPct) {}

    public record TeamFortniteAgg(int playerRows, Double killsPerDeathOrNull, long kills, long deaths, Double avgPlacementOrNull) {}

    public record TeamMlbAgg(
            int playerRows, Double avgBattingAvgGame, long homeRunsSum, Double avgInningsPitched, Double avgEraGame) {}
}
