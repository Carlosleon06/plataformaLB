package com.leonbon.realtime;

import com.leonbon.bets.BetService;
import com.leonbon.notifications.dto.UserNotificationResponse;
import com.leonbon.realtime.dto.MatchBetBoardWsPayload;
import com.leonbon.tournaments.BracketMatch;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeMessagingService {
    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeMessagingService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishMatchBetBoard(BracketMatch match, BetService.MatchStakeBoard board) {
        if (match == null || board == null) return;
        var payload =
                new MatchBetBoardWsPayload(
                        match.getTournamentId(),
                        match.getId(),
                        board.stakeOnA(),
                        board.stakeOnB(),
                        board.impliedReturnPerCoinOnA(),
                        board.impliedReturnPerCoinOnB(),
                        board.bettingClosesAt(),
                        board.bettingWindowMinutes());
        messagingTemplate.convertAndSend(
                "/topic/tournaments/" + match.getTournamentId() + "/matches/" + match.getId() + "/bets", payload);
    }

    public void publishNotificationToUser(String userId, UserNotificationResponse notification) {
        if (userId == null || userId.isBlank() || notification == null) return;
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
    }
}
