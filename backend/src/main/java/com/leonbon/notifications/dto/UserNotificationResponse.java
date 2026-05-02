package com.leonbon.notifications.dto;

import com.leonbon.notifications.NotificationCategory;
import java.time.Instant;

public record UserNotificationResponse(
        String id,
        NotificationCategory category,
        String title,
        String summary,
        String teamIdRef,
        String tournamentIdRef,
        String tournamentEntryIdRef,
        Instant createdAt
) {}
