package com.leonbon.teams;

import org.springframework.security.access.prepost.PreAuthorize;
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
}
