package com.leonbon.teams;

import com.leonbon.teams.dto.PendingTeamAdminRow;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
    public List<PendingTeamAdminRow> listPending() {
        return teamService.listPendingTeamsForAdmin();
    }

    @PostMapping("/{teamId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public void approve(@PathVariable String teamId) {
        teamService.approveTeamAsAdmin(teamId);
    }

    @PostMapping("/{teamId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public void reject(@PathVariable String teamId) {
        teamService.rejectTeamAsAdmin(teamId);
    }

    @PostMapping("/{teamId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public void suspend(@PathVariable String teamId) {
        teamService.suspendTeamAsAdmin(teamId);
    }

    @PostMapping("/{teamId}/logo/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public void resetLogo(@PathVariable String teamId) {
        teamService.resetLogoAdmin(teamId);
    }
}
