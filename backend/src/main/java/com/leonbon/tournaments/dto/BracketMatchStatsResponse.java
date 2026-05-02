package com.leonbon.tournaments.dto;

import com.leonbon.tournaments.GameTitle;
import java.time.Instant;
import java.util.List;

public record BracketMatchStatsResponse(
        String matchId,
        String tournamentId,
        GameTitle game,
        int revision,
        String recordedByAdminUserId,
        Instant recordedAt,
        List<ValorantPlayerStatsOut> valorantPlayers,
        List<FortnitePlayerStatsOut> fortnitePlayers,
        List<MlbPlayerStatsOut> mlbPlayers
) {
    public record ValorantPlayerStatsOut(
            String userId,
            Double kda,
            Integer kills,
            Integer deaths,
            Integer assists,
            Double headshotPct
    ) {}

    public record FortnitePlayerStatsOut(
            String userId,
            Integer kills,
            Integer deaths,
            Integer placement,
            String modePlayed
    ) {}

    public record MlbPlayerStatsOut(
            String userId,
            Double battingAvgGame,
            Integer homeRunsGame,
            Double inningsPitchedGame,
            Double eraGame,
            Integer runsAllowedGame
    ) {}
}
