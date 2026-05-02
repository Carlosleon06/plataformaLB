package com.leonbon.teams.dto;

import com.leonbon.teams.JoinRequestStatus;
import java.time.Instant;

public record JoinRequestResponse(
        String id,
        String teamId,
        String requesterUserId,
        String requesterUsername,
        JoinRequestStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
