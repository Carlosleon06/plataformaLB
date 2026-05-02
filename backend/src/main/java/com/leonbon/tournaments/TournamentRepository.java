package com.leonbon.tournaments;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TournamentRepository extends MongoRepository<Tournament, String> {
    List<Tournament> findTop50ByLifecycleStatusOrderByCompetitionStartAtAsc(TournamentLifecycleStatus status);

    /** Public browse: upcoming / active / past, newest competition window first. */
    List<Tournament> findTop80ByOrderByCompetitionStartAtDesc();

    /** Admin dashboard: recently updated first (e.g. after closing registration). */
    List<Tournament> findTop200ByOrderByUpdatedAtDesc();

    Optional<Tournament> findFirstByNameIgnoreCaseOrderByCreatedAtDesc(String name);
}
