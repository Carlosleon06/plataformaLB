package com.leonbon.users.dto;

import com.leonbon.users.UserStatus;
import com.leonbon.users.UserRole;

public record MeResponse(
        String id,
        String username,
        String nickname,
        UserStatus status,
        UserRole role,
        long leonCoinsBalance
) {}

