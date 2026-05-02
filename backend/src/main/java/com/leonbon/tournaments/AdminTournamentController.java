package com.leonbon.tournaments;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.tournaments.dto.CreateTournamentRequest;
import com.leonbon.tournaments.dto.TournamentResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tournaments")
public class AdminTournamentController {
    private final TournamentService tournamentService;

    public AdminTournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public TournamentResponse create(Authentication auth, @Valid @RequestBody CreateTournamentRequest body) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return tournamentService.createTournamentAsAdmin(p, body);
    }
}
