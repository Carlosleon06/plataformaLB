package com.leonbon.users.dto;

import com.leonbon.users.UserStatus;

public record MeResponse(
        String id,
        String username,
        String nickname,
        UserStatus status,
        long leonCoinsBalance
) {}

