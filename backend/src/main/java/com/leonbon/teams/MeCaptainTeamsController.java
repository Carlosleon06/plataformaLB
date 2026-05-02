package com.leonbon.teams;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.teams.dto.CaptainTeamSummaryResponse;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeCaptainTeamsController {
    private final TeamService teamService;

    public MeCaptainTeamsController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/captain-teams")
    public List<CaptainTeamSummaryResponse> myCaptainTeams(Authentication auth) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        return teamService.listMyApprovedCaptainTeams(principal);
    }
}
