package com.leonbon.tournaments;

import com.leonbon.tournaments.dto.CreateTournamentRequest;
import com.leonbon.tournaments.dto.TournamentResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
    public TournamentResponse create(@Valid @RequestBody CreateTournamentRequest body) {
        return tournamentService.createTournamentAsAdmin(body);
    }
}
