package com.leonbon.bets;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.bets.dto.BetResponse;
import com.leonbon.bets.dto.PlaceBetRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeBetController {
    private final BetService betService;
    private final BetRepository betRepository;

    public MeBetController(BetService betService, BetRepository betRepository) {
        this.betService = betService;
        this.betRepository = betRepository;
    }

    @PostMapping("/bets")
    public BetResponse place(Authentication auth, @Valid @RequestBody PlaceBetRequest body) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        Bet b = betService.placeBet(principal.userId(), body.getMatchId(), body.getPickedEntryId(), body.getAmount());
        return toResponse(b);
    }

    @GetMapping("/bets")
    public List<BetResponse> myBets(
            Authentication auth,
            @RequestParam(required = false) String tournamentId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        int pageSize = Math.max(1, Math.min(200, limit));
        var pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Bet> bets = tournamentId != null && !tournamentId.isBlank()
                ? betRepository.findByUserIdAndTournamentIdOrderByCreatedAtDesc(principal.userId(), tournamentId, pageable)
                : betRepository.findByUserIdOrderByCreatedAtDesc(principal.userId(), pageable);
        return bets.stream().map(MeBetController::toResponse).toList();
    }

    private static BetResponse toResponse(Bet b) {
        return new BetResponse(
                b.getId(),
                b.getTournamentId(),
                b.getMatchId(),
                b.getPickedEntryId(),
                b.getAmount(),
                b.getStatus(),
                b.getPayoutAmount(),
                b.getCreatedAt(),
                b.getResolvedAt()
        );
    }
}

