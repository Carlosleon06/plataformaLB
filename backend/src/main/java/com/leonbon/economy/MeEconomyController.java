package com.leonbon.economy;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.economy.dto.TransactionResponse;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeEconomyController {
    private final TransactionRepository transactionRepository;
    private final EconomyService economyService;

    public MeEconomyController(TransactionRepository transactionRepository, EconomyService economyService) {
        this.transactionRepository = transactionRepository;
        this.economyService = economyService;
    }

    @PostMapping("/daily-claim")
    public TransactionResponse dailyClaim(Authentication auth) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        Transaction t = economyService.claimDaily(principal.userId());
        return toResponse(t);
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> myTransactions(
            Authentication auth,
            @RequestParam(defaultValue = "20") int limit
    ) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        int pageSize = Math.max(1, Math.min(100, limit));
        var pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(principal.userId(), pageable).stream()
                .map(MeEconomyController::toResponse)
                .toList();
    }

    private static TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getType(), t.getAmount(), t.getBalanceAfter(), t.getRef(), t.getCreatedAt());
    }
}

