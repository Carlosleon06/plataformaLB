package com.leonbon.tournaments;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.tournaments.dto.BracketMatchResponse;
import com.leonbon.tournaments.dto.CreateTournamentRequest;
import com.leonbon.tournaments.dto.BracketMatchStatsResponse;
import com.leonbon.tournaments.dto.SetMatchWinnerRequest;
import com.leonbon.tournaments.dto.TournamentResponse;
import com.leonbon.tournaments.dto.UpsertBracketMatchStatsRequest;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tournaments")
public class AdminTournamentController {
    private final TournamentService tournamentService;
    private final TournamentBracketService tournamentBracketService;
    private final MatchStatsService matchStatsService;

    public AdminTournamentController(
            TournamentService tournamentService,
            TournamentBracketService tournamentBracketService,
            MatchStatsService matchStatsService
    ) {
        this.tournamentService = tournamentService;
        this.tournamentBracketService = tournamentBracketService;
        this.matchStatsService = matchStatsService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TournamentResponse> listForAdmin(Authentication auth) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return tournamentService.listTournamentsForAdmin(p);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public TournamentResponse create(Authentication auth, @Valid @RequestBody CreateTournamentRequest body) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return tournamentService.createTournamentAsAdmin(p, body);
    }

    @PostMapping("/{tournamentId}/registration/close")
    @PreAuthorize("isAuthenticated()")
    public TournamentResponse closeRegistration(Authentication auth, @PathVariable String tournamentId) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return tournamentBracketService.closeRegistrationAsAdmin(p, tournamentId);
    }

    @PostMapping("/{tournamentId}/registration/reopen")
    @PreAuthorize("isAuthenticated()")
    public TournamentResponse reopenRegistration(Authentication auth, @PathVariable String tournamentId) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return tournamentBracketService.reopenRegistrationAsAdmin(p, tournamentId);
    }

    @DeleteMapping("/{tournamentId}")
    @PreAuthorize("isAuthenticated()")
    public void deleteTournament(Authentication auth, @PathVariable String tournamentId) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        tournamentBracketService.deleteTournamentAsAdmin(p, tournamentId);
    }

    @PostMapping("/{tournamentId}/bracket/generate")
    @PreAuthorize("isAuthenticated()")
    public TournamentResponse generateBracket(Authentication auth, @PathVariable String tournamentId) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return tournamentBracketService.generateBracketAsAdmin(p, tournamentId);
    }

    @PostMapping("/{tournamentId}/matches/{matchId}/betting/open")
    @PreAuthorize("isAuthenticated()")
    public BracketMatchResponse openBettingWindow(
            Authentication auth, @PathVariable String tournamentId, @PathVariable String matchId) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return tournamentBracketService.openBettingWindowAsAdmin(p, tournamentId, matchId);
    }

    @PostMapping("/{tournamentId}/matches/{matchId}/betting/close")
    @PreAuthorize("isAuthenticated()")
    public BracketMatchResponse closeBettingWindow(
            Authentication auth, @PathVariable String tournamentId, @PathVariable String matchId) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return tournamentBracketService.closeBettingWindowAsAdmin(p, tournamentId, matchId);
    }

    @PostMapping("/{tournamentId}/matches/{matchId}/winner")
    @PreAuthorize("isAuthenticated()")
    public BracketMatchResponse setMatchWinner(
            Authentication auth,
            @PathVariable String tournamentId,
            @PathVariable String matchId,
            @Valid @RequestBody SetMatchWinnerRequest body
    ) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return tournamentBracketService.setMatchWinnerAsAdmin(p, tournamentId, matchId, body.getWinnerEntryId());
    }

    @PutMapping("/{tournamentId}/matches/{matchId}/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> upsertMatchStats(
            Authentication auth,
            @PathVariable String tournamentId,
            @PathVariable String matchId,
            @Valid @RequestBody UpsertBracketMatchStatsRequest body
    ) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        Optional<BracketMatchStatsResponse> out =
                matchStatsService.upsertAsAdmin(p, tournamentId, matchId, body);
        return out.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
}
