package com.leonbon.tournaments;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BracketMatchRepository extends MongoRepository<BracketMatch, String> {
    long countByTournamentId(String tournamentId);

    void deleteByTournamentId(String tournamentId);

    List<BracketMatch> findByTournamentIdOrderByRoundAscIndexInRoundAsc(String tournamentId);

    Optional<BracketMatch> findByTournamentIdAndRoundAndIndexInRound(String tournamentId, int round, int indexInRound);
}
