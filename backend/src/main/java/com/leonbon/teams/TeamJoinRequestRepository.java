package com.leonbon.teams;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeamJoinRequestRepository extends MongoRepository<TeamJoinRequest, String> {
    Optional<TeamJoinRequest> findByTeamIdAndRequesterUserIdAndStatus(String teamId, String requesterUserId, JoinRequestStatus status);

    List<TeamJoinRequest> findByTeamIdAndStatusOrderByCreatedAtAsc(String teamId, JoinRequestStatus status);
}
