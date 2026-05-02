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
        Instant createdAt,
        /** Resolved team name when {@code type} is TEAM; null otherwise. */
        String teamName,
        /** Resolved team tag when {@code type} is TEAM; null otherwise. */
        String teamTag,
        /** Resolved username when {@code type} is PLAYER; null otherwise. */
        String playerUsername
) {}
