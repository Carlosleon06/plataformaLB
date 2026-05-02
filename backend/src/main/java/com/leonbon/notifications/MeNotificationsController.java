package com.leonbon.notifications;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.notifications.dto.UserNotificationResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeNotificationsController {
    private final NotificationService notificationService;

    public MeNotificationsController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public List<UserNotificationResponse> list(
            Authentication auth,
            @RequestParam(required = false) Instant after,
            @RequestParam(defaultValue = "30") int limit
    ) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return notificationService.listSince(principal.userId(), after, limit);
    }
}
