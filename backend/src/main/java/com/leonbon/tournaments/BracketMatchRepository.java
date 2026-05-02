package com.leonbon.tournaments;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BracketMatchRepository extends MongoRepository<BracketMatch, String> {
    long countByTournamentId(String tournamentId);

    void deleteByTournamentId(String tournamentId);

    List<BracketMatch> findByTournamentIdOrderByBracketPoolAscRoundAscIndexInRoundAsc(String tournamentId);

    List<BracketMatch> findByTournamentIdInAndStatus(
            Collection<String> tournamentIds, BracketMatchStatus status);

    Optional<BracketMatch> findByTournamentIdAndBracketPoolAndRoundAndIndexInRound(
            String tournamentId, BracketPool bracketPool, int round, int indexInRound);

    /** @deprecated use {@link #findByTournamentIdAndBracketPoolAndRoundAndIndexInRound} with {@link BracketPool#WB} */
    @Deprecated
    Optional<BracketMatch> findByTournamentIdAndRoundAndIndexInRound(String tournamentId, int round, int indexInRound);

    List<BracketMatch> findByBettingWindowClosesAtLessThanEqual(Instant instant);
}
