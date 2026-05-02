package com.leonbon.teams.dto;

import java.util.List;

public record CaptainTeamSummaryResponse(
        String id,
        String name,
        String tag,
        String regionServer,
        String logoUrl,
        List<String> memberUserIds
) {}
