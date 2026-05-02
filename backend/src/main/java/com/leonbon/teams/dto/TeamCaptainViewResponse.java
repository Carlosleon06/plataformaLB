package com.leonbon.teams.dto;

import com.leonbon.teams.TeamStatus;
import java.time.Instant;
import java.util.List;

public record TeamCaptainViewResponse(
        String id,
        String name,
        String tag,
        String regionServer,
        String logoUrl,
        TeamStatus status,
        int memberCount,
        Instant createdAt,
        String captainUserId,
        String captainUsername,
        List<String> coachUserIds,
        List<String> coachUsernames,
        List<String> memberUserIds,
        List<String> memberUsernames
) {}
