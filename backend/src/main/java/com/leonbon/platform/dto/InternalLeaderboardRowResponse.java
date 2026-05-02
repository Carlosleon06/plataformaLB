package com.leonbon.platform.dto;

public record InternalLeaderboardRowResponse(
        String userId,
        String username,
        String nickname,
        int bracketMatchWins
) {}
