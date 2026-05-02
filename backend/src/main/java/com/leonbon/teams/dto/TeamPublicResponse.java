package com.leonbon.teams.dto;

import com.leonbon.teams.TeamStatus;
import java.time.Instant;
import java.util.List;

public record TeamPublicResponse(
        String id,
        String name,
        String tag,
        String regionServer,
        String logoUrl,
        TeamStatus status,
        int memberCount,
        Instant createdAt,
        List<String> sponsorLines,
        String canonicalStreamUrl,
        TeamCompetitionSummaryResponse competitionSummaryOrNull
) {}
