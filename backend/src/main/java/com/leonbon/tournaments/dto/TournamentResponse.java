package com.leonbon.tournaments.dto;

import com.leonbon.tournaments.GameTitle;
import com.leonbon.tournaments.TournamentFormat;
import com.leonbon.tournaments.TournamentLifecycleStatus;
import java.time.Instant;
import java.util.List;

public record TournamentResponse(
        String id,
        String name,
        String organizers,
        GameTitle game,
        TournamentFormat format,
        TournamentLifecycleStatus lifecycleStatus,
        Instant registrationStartAt,
        Instant registrationEndAt,
        Instant competitionStartAt,
        Instant competitionEndAt,
        String streamUrl,
        String rulesHtml,
        String eligibilityNotes,
        String prizeNotes,
        Integer prizeWinnerSlots,
        List<Long> prizeLeonCoinsByPlacement,
        Integer maxApprovedParticipants,
        Integer bracketSize,
        Instant placementPrizeLedgerCompletedAt,
        Instant createdAt
) {}
