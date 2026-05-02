package com.leonbon.bets;

import com.leonbon.tournaments.BracketMatch;
import com.leonbon.tournaments.BracketMatchRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Limpia ventanas caducadas (cierre manual + auto) y notifica WS. */
@Component
public class BettingWindowExpiryTask {
    private final BracketMatchRepository bracketMatchRepository;
    private final BetService betService;

    public BettingWindowExpiryTask(BracketMatchRepository bracketMatchRepository, BetService betService) {
        this.bracketMatchRepository = bracketMatchRepository;
        this.betService = betService;
    }

    @Scheduled(fixedDelayString = "${app.bets.windowExpirySweepMs:10000}")
    public void expireDueWindows() {
        Instant now = Instant.now();
        List<BracketMatch> due = bracketMatchRepository.findByBettingWindowClosesAtLessThanEqual(now);
        for (BracketMatch m : due) {
            m.setBettingWindowClosesAt(null);
            m.setUpdatedAt(now);
            bracketMatchRepository.save(m);
            betService.publishStakeBoard(m);
        }
    }
}
