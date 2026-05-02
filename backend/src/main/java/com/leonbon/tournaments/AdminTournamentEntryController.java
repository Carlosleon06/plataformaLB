package com.leonbon.tournaments;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.tournaments.dto.TournamentEntryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    @PreAuthorize("isAuthenticated()")
    public TournamentEntryResponse approve(Authentication auth, @PathVariable String tournamentId, @PathVariable String entryId) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return tournamentService.approveEntryAsAdmin(p, tournamentId, entryId);
    }

    @PostMapping("/{tournamentId}/entries/{entryId}/reject")
    @PreAuthorize("isAuthenticated()")
    public TournamentEntryResponse reject(Authentication auth, @PathVariable String tournamentId, @PathVariable String entryId) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return tournamentService.rejectEntryAsAdmin(p, tournamentId, entryId);
    }
}
