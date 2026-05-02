package com.leonbon.teams.dto;

import com.leonbon.teams.TeamStatus;
import java.time.Instant;

public record PendingTeamAdminRow(
        String id,
        String name,
        String tag,
        TeamStatus status,
        String regionServer,
        String captainUsername,
        int memberCount,
        Instant createdAt
) {}
