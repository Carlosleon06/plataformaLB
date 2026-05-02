package com.leonbon.trophies;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TrophyAwardRepository extends MongoRepository<TrophyAward, String> {
    boolean existsByTournamentId(String tournamentId);

    List<TrophyAward> findByTournamentIdOrderByPlacementAsc(String tournamentId);

    List<TrophyAward> findByPlayerIdOrderByAwardedAtDesc(String playerId);

    List<TrophyAward> findByTeamIdOrderByAwardedAtDesc(String teamId);

    /** Palmarés de un jugador: victorias 1vs1 + integrante de roster equipado cuando el equipo figura premiado. */
    List<TrophyAward> findByCreditedMemberUserIdsContainingOrderByAwardedAtDesc(String memberUserId);

    Optional<TrophyAward> findFirstByTournamentIdAndPlacement(String tournamentId, int placement);
}
