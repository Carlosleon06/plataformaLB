package com.leonbon.tournaments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CreateTeamTournamentEntryRequest {
    @NotBlank
    private String teamId;

    @NotEmpty
    private List<@NotBlank String> selectedRosterUserIds;

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public List<String> getSelectedRosterUserIds() {
        return selectedRosterUserIds;
    }

    public void setSelectedRosterUserIds(List<String> selectedRosterUserIds) {
        this.selectedRosterUserIds = selectedRosterUserIds;
    }
}
