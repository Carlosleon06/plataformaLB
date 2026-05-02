package com.leonbon.tournaments.stats;

import com.leonbon.tournaments.GameTitle;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BracketMatchStatsRepository extends MongoRepository<BracketMatchStats, String> {
    Optional<BracketMatchStats> findByMatchId(String matchId);

    List<BracketMatchStats> findByGame(GameTitle game);

    List<BracketMatchStats> findByTournamentIdIn(Collection<String> tournamentIds);

    void deleteByMatchId(String matchId);
}
