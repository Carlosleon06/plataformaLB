package com.leonbon.tournaments;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.tournaments.dto.BracketMatchResponse;
import com.leonbon.tournaments.dto.CreateTeamTournamentEntryRequest;
import com.leonbon.tournaments.dto.TournamentEntryResponse;
import com.leonbon.tournaments.dto.TournamentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {
    private final TournamentService tournamentService;
    private final TournamentBracketService tournamentBracketService;

    public TournamentController(TournamentService tournamentService, TournamentBracketService tournamentBracketService) {
        this.tournamentService = tournamentService;
        this.tournamentBracketService = tournamentBracketService;
    }

    @GetMapping
    public List<TournamentResponse> list() {
        return tournamentService.listPublicTournaments();
    }

    @GetMapping("/{tournamentId}")
    public TournamentResponse get(@PathVariable String tournamentId) {
        return tournamentService.getPublicTournament(tournamentId);
    }

    @GetMapping("/{tournamentId}/entries")
    public List<TournamentEntryResponse> listEntries(@PathVariable String tournamentId) {
        return tournamentService.listEntries(tournamentId);
    }

    @GetMapping("/{tournamentId}/matches")
    public List<BracketMatchResponse> listMatches(@PathVariable String tournamentId) {
        return tournamentBracketService.listBracketMatches(tournamentId);
    }

    @PostMapping("/{tournamentId}/entries/team")
    public TournamentEntryResponse createTeamEntry(
            Authentication auth,
            @PathVariable String tournamentId,
            @Valid @RequestBody CreateTeamTournamentEntryRequest body
    ) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return tournamentService.createTeamEntry(principal, tournamentId, body);
    }

    @PostMapping("/{tournamentId}/entries/me")
    public TournamentEntryResponse createMlbPlayerEntry(Authentication auth, @PathVariable String tournamentId) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return tournamentService.createMlbPlayerEntry(principal, tournamentId);
    }
}
