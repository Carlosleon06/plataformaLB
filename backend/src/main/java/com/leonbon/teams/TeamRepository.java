package com.leonbon.teams;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeamRepository extends MongoRepository<Team, String> {
    boolean existsByNameIgnoreCase(String name);

    Optional<Team> findByNameIgnoreCase(String name);

    List<Team> findTop20ByStatusOrderByCreatedAtDesc(TeamStatus status);

    List<Team> findTop50ByStatusAndNameContainingIgnoreCaseOrderByNameAsc(TeamStatus status, String query);

    List<Team> findByCaptainUserIdAndStatusOrderByNameAsc(String captainUserId, TeamStatus status);

    List<Team> findTop100ByStatusOrderByCreatedAtAsc(TeamStatus status);

    List<Team> findByMemberUserIdsContainingAndStatusOrderByNameAsc(String memberUserId, TeamStatus status);
}
