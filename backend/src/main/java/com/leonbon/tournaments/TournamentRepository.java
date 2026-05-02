package com.leonbon.tournaments;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TournamentRepository extends MongoRepository<Tournament, String> {
    List<Tournament> findTop50ByLifecycleStatusOrderByCompetitionStartAtAsc(TournamentLifecycleStatus status);

    Optional<Tournament> findFirstByNameIgnoreCaseOrderByCreatedAtDesc(String name);
}
