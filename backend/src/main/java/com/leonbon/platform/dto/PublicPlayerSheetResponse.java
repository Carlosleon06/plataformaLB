package com.leonbon.platform.dto;

import com.leonbon.users.dto.PlayerSocialLinksResponse;
import java.util.List;
import java.util.Map;

/** Perfil público opcionalmente enriquecido (sin email). */

public record PublicPlayerSheetResponse(
        Long leonPlayerNumber,
        String publicFullNameOrNull,
        String country,
        PlayerSocialLinksResponse socialLinks,
        String preferredGame,
        Map<String, String> rankLabelsByGame,
        List<TeamAffiliationPublicResponse> approvedTeamAffiliations
) {}
