package com.leonbon.platform;

import com.leonbon.platform.dto.InternalLeaderboardRowResponse;
import com.leonbon.platform.dto.UserPlatformSnapshotResponse;
import com.leonbon.trophies.TrophyAwardIssuanceService;
import com.leonbon.trophies.dto.TrophyAwardResponse;
import com.leonbon.tournaments.GameTitle;
import com.leonbon.web.BadRequestException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform")
public class PlatformController {
    private final PlatformProfileService platformProfileService;
    private final TrophyAwardIssuanceService trophyAwardIssuanceService;

    public PlatformController(
            PlatformProfileService platformProfileService, TrophyAwardIssuanceService trophyAwardIssuanceService) {
        this.platformProfileService = platformProfileService;
        this.trophyAwardIssuanceService = trophyAwardIssuanceService;
    }

    @GetMapping("/users/{userId}/snapshot")
    public UserPlatformSnapshotResponse userSnapshot(@PathVariable String userId) {
        return platformProfileService.userSnapshot(userId);
    }

    @GetMapping("/users/{userId}/trophies")
    public List<TrophyAwardResponse> userTrophies(@PathVariable String userId) {
        return TrophyAwardIssuanceService.mapResponses(trophyAwardIssuanceService.listMergedForMemberUser(userId));
    }

    @GetMapping("/leaderboards/{game}")
    public List<InternalLeaderboardRowResponse> leaderboard(
            @PathVariable String game, @RequestParam(defaultValue = "10") int limit
    ) {
        GameTitle g = parseGame(game);
        return platformProfileService.leaderboard(g, limit);
    }

    private static GameTitle parseGame(String raw) {
        if (raw == null || raw.isBlank()) throw new BadRequestException("game required");
        try {
            return GameTitle.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("unknown game '" + raw + "'");
        }
    }
}
