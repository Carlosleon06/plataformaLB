package com.leonbon.platform.dto;

public record TeamAffiliationPublicResponse(
        String teamId,
        String name,
        String tag,
        boolean captain,
        String regionServer,
        String logoUrl
) {}
