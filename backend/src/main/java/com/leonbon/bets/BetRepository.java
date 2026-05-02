package com.leonbon.bets;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BetRepository extends MongoRepository<Bet, String> {
    List<Bet> findByMatchId(String matchId);

    List<Bet> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Bet> findByUserIdAndTournamentIdOrderByCreatedAtDesc(String userId, String tournamentId, Pageable pageable);

    List<Bet> findByMatchIdAndStatus(String matchId, BetStatus status);
}

