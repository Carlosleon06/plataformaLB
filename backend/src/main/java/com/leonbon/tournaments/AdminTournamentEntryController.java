package com.leonbon.tournaments;

import com.leonbon.tournaments.dto.TournamentEntryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tournaments")
public class AdminTournamentEntryController {
    private final TournamentService tournamentService;

    public AdminTournamentEntryController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @PostMapping("/{tournamentId}/entries/{entryId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public TournamentEntryResponse approve(@PathVariable String tournamentId, @PathVariable String entryId) {
        return tournamentService.approveEntryAsAdmin(tournamentId, entryId);
    }

    @PostMapping("/{tournamentId}/entries/{entryId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public TournamentEntryResponse reject(@PathVariable String tournamentId, @PathVariable String entryId) {
        return tournamentService.rejectEntryAsAdmin(tournamentId, entryId);
    }
}
