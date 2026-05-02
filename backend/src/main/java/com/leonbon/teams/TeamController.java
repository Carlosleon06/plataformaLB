package com.leonbon.teams;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.trophies.TrophyAwardIssuanceService;
import com.leonbon.trophies.dto.TrophyAwardResponse;
import com.leonbon.teams.dto.TeamCollectiveBracketStatsResponse;
import com.leonbon.teams.dto.CreateTeamRequest;
import com.leonbon.teams.dto.DelegateCaptainRequest;
import com.leonbon.teams.dto.JoinRequestResponse;
import com.leonbon.teams.dto.TeamCaptainViewResponse;
import com.leonbon.teams.dto.TeamPublicResponse;
import com.leonbon.teams.dto.PatchCaptainTeamPresenceRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/teams")
public class TeamController {
    private final TeamService teamService;
    private final TeamCollectiveBracketStatsService teamCollectiveBracketStatsService;
    private final TrophyAwardIssuanceService trophyAwardIssuanceService;

    public TeamController(
            TeamService teamService,
            TeamCollectiveBracketStatsService teamCollectiveBracketStatsService,
            TrophyAwardIssuanceService trophyAwardIssuanceService) {
        this.teamService = teamService;
        this.teamCollectiveBracketStatsService = teamCollectiveBracketStatsService;
        this.trophyAwardIssuanceService = trophyAwardIssuanceService;
    }

    @GetMapping("/public")
    public List<TeamPublicResponse> listPublic() {
        return teamService.listApprovedTeams();
    }

    @GetMapping("/public/search")
    public List<TeamPublicResponse> searchPublic(@RequestParam("q") String q) {
        return teamService.searchApprovedTeams(q);
    }

    @GetMapping("/public/{teamId}")
    public TeamPublicResponse publicTeam(@PathVariable String teamId) {
        return teamService.getPublicTeam(teamId);
    }

    @GetMapping("/public/{teamId}/collective-bracket-stats")
    public TeamCollectiveBracketStatsResponse collectiveBracketStats(@PathVariable String teamId) {
        return teamCollectiveBracketStatsService.summarize(teamId);
    }

    @GetMapping("/public/{teamId}/trophies")
    public List<TrophyAwardResponse> teamTrophies(@PathVariable String teamId) {
        return TrophyAwardIssuanceService.mapResponses(trophyAwardIssuanceService.listForTeamMembersView(teamId));
    }

    @PostMapping
    public TeamPublicResponse create(Authentication auth, @Valid @RequestBody CreateTeamRequest req) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.createTeam(principal, req);
    }

    @GetMapping("/{teamId}")
    public Object get(Authentication auth, @PathVariable String teamId) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.getTeamForViewer(principal, teamId);
    }

    @PostMapping("/{teamId}/join-requests")
    public JoinRequestResponse requestJoin(Authentication auth, @PathVariable String teamId) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.requestJoin(principal, teamId);
    }

    @GetMapping("/{teamId}/join-requests")
    public List<JoinRequestResponse> listJoinRequests(Authentication auth, @PathVariable String teamId) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.listPendingJoinRequests(principal, teamId);
    }

    @PostMapping("/{teamId}/join-requests/{requestId}/accept")
    public JoinRequestResponse accept(Authentication auth, @PathVariable String teamId, @PathVariable String requestId) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.respondJoinRequest(principal, teamId, requestId, JoinRequestStatus.ACCEPTED);
    }

    @PostMapping("/{teamId}/join-requests/{requestId}/reject")
    public JoinRequestResponse reject(Authentication auth, @PathVariable String teamId, @PathVariable String requestId) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.respondJoinRequest(principal, teamId, requestId, JoinRequestStatus.REJECTED);
    }

    @PostMapping("/{teamId}/captain/delegate")
    public TeamCaptainViewResponse delegate(
            Authentication auth,
            @PathVariable String teamId,
            @Valid @RequestBody DelegateCaptainRequest body
    ) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.delegateCaptain(principal, teamId, body.newCaptainUserId());
    }

    @PostMapping("/{teamId}/coaches/{userId}")
    public TeamCaptainViewResponse addCoach(Authentication auth, @PathVariable String teamId, @PathVariable String userId) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.addCoach(principal, teamId, userId);
    }

    @PostMapping("/{teamId}/coaches/{userId}/remove")
    public TeamCaptainViewResponse removeCoach(Authentication auth, @PathVariable String teamId, @PathVariable String userId) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.removeCoach(principal, teamId, userId);
    }

    @PostMapping(value = "/{teamId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TeamCaptainViewResponse uploadLogo(Authentication auth, @PathVariable String teamId, @RequestPart("file") MultipartFile file) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.uploadLogo(principal, teamId, file);
    }

    @PostMapping("/{teamId}/logo/reset")
    public TeamCaptainViewResponse resetLogoCaptain(Authentication auth, @PathVariable String teamId) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.resetLogoCaptain(principal, teamId);
    }

    @PatchMapping("/{teamId}/captain/commercial-presence")
    public TeamCaptainViewResponse patchCaptainCommercial(
            Authentication auth,
            @PathVariable String teamId,
            @Valid @RequestBody PatchCaptainTeamPresenceRequest body
    ) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.patchCaptainCommercialFields(principal, teamId, body);
    }

    @PostMapping("/{teamId}/leave")
    public TeamCaptainViewResponse leave(Authentication auth, @PathVariable String teamId) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.leaveTeam(principal, teamId);
    }

    @PostMapping("/{teamId}/disband")
    public TeamCaptainViewResponse disband(Authentication auth, @PathVariable String teamId) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.disbandTeamIfSoleCaptain(principal, teamId);
    }
}
