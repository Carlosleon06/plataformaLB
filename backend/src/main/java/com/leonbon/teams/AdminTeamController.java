package com.leonbon.teams;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.teams.dto.PendingTeamAdminRow;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/teams")
public class AdminTeamController {
    private final TeamService teamService;

    public AdminTeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public List<PendingTeamAdminRow> listPending(Authentication auth) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        return teamService.listPendingTeamsForAdmin(p);
    }

    @PostMapping("/{teamId}/approve")
    @PreAuthorize("isAuthenticated()")
    public void approve(Authentication auth, @PathVariable String teamId) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        teamService.approveTeamAsAdmin(p, teamId);
    }

    @PostMapping("/{teamId}/reject")
    @PreAuthorize("isAuthenticated()")
    public void reject(Authentication auth, @PathVariable String teamId) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        teamService.rejectTeamAsAdmin(p, teamId);
    }

    @PostMapping("/{teamId}/suspend")
    @PreAuthorize("isAuthenticated()")
    public void suspend(Authentication auth, @PathVariable String teamId) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        teamService.suspendTeamAsAdmin(p, teamId);
    }

    @PostMapping("/{teamId}/logo/reset")
    @PreAuthorize("isAuthenticated()")
    public void resetLogo(Authentication auth, @PathVariable String teamId) {
        JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
        teamService.resetLogoAdmin(p, teamId);
    }
}
