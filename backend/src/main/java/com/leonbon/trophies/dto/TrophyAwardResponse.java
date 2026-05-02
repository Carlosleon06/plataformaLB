package com.leonbon.trophies.dto;

import com.leonbon.tournaments.GameTitle;
import com.leonbon.tournaments.TournamentFormat;
import com.leonbon.tournaments.TournamentEntryType;
import java.time.Instant;

public record TrophyAwardResponse(
        String id,
        String tournamentId,
        String tournamentName,
        GameTitle game,
        TournamentFormat tournamentFormat,
        int placement,
        String badgeLabel,
        String tournamentEntryId,
        TournamentEntryType entryType,
        String teamId,
        String playerId,
        Instant awardedAt
) {}
