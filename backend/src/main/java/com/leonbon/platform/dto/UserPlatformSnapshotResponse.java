package com.leonbon.platform.dto;

import java.util.List;

public record UserPlatformSnapshotResponse(
        String userId,
        String username,
        String nickname,
        PublicPlayerSheetResponse publicSheet,
        List<GamePlatformRollupResponse> games
) {}
