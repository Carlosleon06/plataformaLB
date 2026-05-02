package com.leonbon.notifications;

import com.leonbon.notifications.dto.UserNotificationResponse;
import com.leonbon.realtime.RealtimeMessagingService;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final UserNotificationRepository userNotificationRepository;
    private final RealtimeMessagingService realtimeMessagingService;

    public NotificationService(
            UserNotificationRepository userNotificationRepository,
            RealtimeMessagingService realtimeMessagingService) {
        this.userNotificationRepository = userNotificationRepository;
        this.realtimeMessagingService = realtimeMessagingService;
    }

    private void persistAndPush(UserNotification n) {
        UserNotification saved = userNotificationRepository.save(n);
        realtimeMessagingService.publishNotificationToUser(saved.getUserId(), toResponse(saved));
    }

    public void publishTeamJoinAccepted(String recipientUserId, String teamId, String teamName) {
        UserNotification n = new UserNotification();
        n.setUserId(recipientUserId);
        n.setCategory(NotificationCategory.TEAM_JOIN_ACCEPTED);
        n.setTitle("Solicitud aceptada");
        n.setSummary("Te incorporaste al equipo «" + teamName + "».");
        n.setTeamIdRef(teamId);
        n.setCreatedAt(Instant.now());
        persistAndPush(n);
    }

    public void publishTeamJoinRejected(String recipientUserId, String teamId, String teamName) {
        UserNotification n = new UserNotification();
        n.setUserId(recipientUserId);
        n.setCategory(NotificationCategory.TEAM_JOIN_REJECTED);
        n.setTitle("Solicitud rechazada");
        n.setSummary("El capitán rechazó tu ingreso a «" + teamName + "».");
        n.setTeamIdRef(teamId);
        n.setCreatedAt(Instant.now());
        persistAndPush(n);
    }

    public void publishTournamentEntryApproved(
            String recipientUserId, String tournamentName, String tournamentId, String entryId) {
        UserNotification n = new UserNotification();
        n.setUserId(recipientUserId);
        n.setCategory(NotificationCategory.TOURNAMENT_ENTRY_APPROVED);
        n.setTitle("Inscripción aprobada");
        n.setSummary("Tu entrada fue aprobada en «" + tournamentName + "».");
        n.setTournamentIdRef(tournamentId);
        n.setTournamentEntryIdRef(entryId);
        n.setCreatedAt(Instant.now());
        persistAndPush(n);
    }

    public void publishTournamentEntryRejected(
            String recipientUserId, String tournamentName, String tournamentId, String entryId) {
        UserNotification n = new UserNotification();
        n.setUserId(recipientUserId);
        n.setCategory(NotificationCategory.TOURNAMENT_ENTRY_REJECTED);
        n.setTitle("Inscripción rechazada");
        n.setSummary("Tu entrada fue rechazada en «" + tournamentName + "».");
        n.setTournamentIdRef(tournamentId);
        n.setTournamentEntryIdRef(entryId);
        n.setCreatedAt(Instant.now());
        persistAndPush(n);
    }

    public List<UserNotificationResponse> listSince(String userId, Instant since, int limit) {
        int pageSize = Math.max(1, Math.min(limit, 100));
        var pageable = PageRequest.of(0, pageSize);
        Instant cursor = since == null ? Instant.EPOCH : since;
        return userNotificationRepository
                .findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, cursor, pageable)
                .stream()
                .map(NotificationService::toResponse)
                .toList();
    }

    private static UserNotificationResponse toResponse(UserNotification n) {
        return new UserNotificationResponse(
                n.getId(),
                n.getCategory(),
                n.getTitle(),
                n.getSummary(),
                n.getTeamIdRef(),
                n.getTournamentIdRef(),
                n.getTournamentEntryIdRef(),
                n.getCreatedAt());
    }
}
