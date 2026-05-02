package com.leonbon.notifications;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_notifications")
public class UserNotification {
    @Id
    private String id;

    @Indexed
    private String userId;

    private NotificationCategory category;

    private String title;

    /** Texto corto apto para toast. */
    private String summary;

    private String teamIdRef;

    private String tournamentIdRef;

    private String tournamentEntryIdRef;

    private Instant createdAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public NotificationCategory getCategory() {
        return category;
    }

    public void setCategory(NotificationCategory category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getTeamIdRef() {
        return teamIdRef;
    }

    public void setTeamIdRef(String teamIdRef) {
        this.teamIdRef = teamIdRef;
    }

    public String getTournamentIdRef() {
        return tournamentIdRef;
    }

    public void setTournamentIdRef(String tournamentIdRef) {
        this.tournamentIdRef = tournamentIdRef;
    }

    public String getTournamentEntryIdRef() {
        return tournamentEntryIdRef;
    }

    public void setTournamentEntryIdRef(String tournamentEntryIdRef) {
        this.tournamentEntryIdRef = tournamentEntryIdRef;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
