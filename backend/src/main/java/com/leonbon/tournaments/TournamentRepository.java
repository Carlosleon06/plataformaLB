package com.leonbon.tournaments;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TournamentRepository extends MongoRepository<Tournament, String> {
    List<Tournament> findTop50ByLifecycleStatusOrderByCompetitionStartAtAsc(TournamentLifecycleStatus status);
}
