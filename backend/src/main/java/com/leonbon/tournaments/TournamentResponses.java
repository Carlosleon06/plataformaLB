package com.leonbon.tournaments;

import com.leonbon.tournaments.dto.TournamentResponse;
import java.util.List;

public final class TournamentResponses {
    private TournamentResponses() {}

    public static TournamentResponse from(Tournament t) {
        return new TournamentResponse(
                t.getId(),
                t.getName(),
                t.getOrganizers(),
                t.getGame(),
                t.getFormat(),
                t.getLifecycleStatus(),
                t.isRegistrationManuallyOpened(),
                t.getRegistrationStartAt(),
                t.getRegistrationEndAt(),
                t.getCompetitionStartAt(),
                t.getCompetitionEndAt(),
                t.getStreamUrl(),
                t.getRulesHtml(),
                t.getEligibilityNotes(),
                t.getPrizeNotes(),
                t.getPrizeWinnerSlots(),
                t.getPrizeLeonCoinsByPlacement() == null ? null : List.copyOf(t.getPrizeLeonCoinsByPlacement()),
                t.getMaxApprovedParticipants(),
                t.getBracketSize(),
                t.getPlacementPrizeLedgerCompletedAt(),
                t.getCreatedAt());
    }
}
