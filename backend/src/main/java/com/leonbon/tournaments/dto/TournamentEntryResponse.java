package com.leonbon.tournaments.dto;

import com.leonbon.tournaments.TournamentEntryStatus;
import com.leonbon.tournaments.TournamentEntryType;
import java.time.Instant;
import java.util.List;

public record TournamentEntryResponse(
        String id,
        String tournamentId,
        TournamentEntryType type,
        String teamId,
        String playerId,
        TournamentEntryStatus status,
        List<String> selectedRosterUserIds,
        Instant createdAt
) {}
