package com.leonbon.users.dto;

import com.leonbon.users.UserRole;
import com.leonbon.users.UserStatus;
import java.util.Map;

public record MeResponse(
        String id,
        Long leonPlayerNumber,
        String username,
        String email,
        String nickname,
        String fullName,
        boolean profileShowFullName,
        String country,
        PlayerSocialLinksResponse socialLinks,
        String preferredGame,
        Map<String, String> rankLabelsByGame,
        UserStatus status,
        UserRole role,
        long leonCoinsBalance
) {}
