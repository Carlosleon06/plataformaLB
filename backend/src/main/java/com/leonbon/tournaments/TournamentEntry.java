package com.leonbon.tournaments;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("tournament_entries")
public class TournamentEntry {
    @Id
    private String id;

    @Indexed
    private String tournamentId;

    private TournamentEntryType type;

    private String teamId; // TEAM entries

    private String playerId; // PLAYER entries (MLB)

    private TournamentEntryStatus status = TournamentEntryStatus.PENDING;

    private List<String> selectedRosterUserIds = new ArrayList<>();

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(String tournamentId) {
        this.tournamentId = tournamentId;
    }

    public TournamentEntryType getType() {
        return type;
    }

    public void setType(TournamentEntryType type) {
        this.type = type;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public TournamentEntryStatus getStatus() {
        return status;
    }

    public void setStatus(TournamentEntryStatus status) {
        this.status = status;
    }

    public List<String> getSelectedRosterUserIds() {
        return selectedRosterUserIds;
    }

    public void setSelectedRosterUserIds(List<String> selectedRosterUserIds) {
        this.selectedRosterUserIds = selectedRosterUserIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
