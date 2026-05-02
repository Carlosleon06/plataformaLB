package com.leonbon.tournaments;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("tournaments")
public class Tournament {
    @Id
    private String id;

    private String name;

    private String organizers;

    private GameTitle game;

    private TournamentFormat format;

    private TournamentLifecycleStatus lifecycleStatus = TournamentLifecycleStatus.REGISTRATION_OPEN;

    private Instant registrationStartAt;
    private Instant registrationEndAt;

    private Instant competitionStartAt;
    private Instant competitionEndAt;

    private String streamUrl;

    /** Power-of-two bracket size after generation; null before bracket exists. */
    private Integer bracketSize;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrganizers() {
        return organizers;
    }

    public void setOrganizers(String organizers) {
        this.organizers = organizers;
    }

    public GameTitle getGame() {
        return game;
    }

    public void setGame(GameTitle game) {
        this.game = game;
    }

    public TournamentFormat getFormat() {
        return format;
    }

    public void setFormat(TournamentFormat format) {
        this.format = format;
    }

    public TournamentLifecycleStatus getLifecycleStatus() {
        return lifecycleStatus;
    }

    public void setLifecycleStatus(TournamentLifecycleStatus lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
    }

    public Instant getRegistrationStartAt() {
        return registrationStartAt;
    }

    public void setRegistrationStartAt(Instant registrationStartAt) {
        this.registrationStartAt = registrationStartAt;
    }

    public Instant getRegistrationEndAt() {
        return registrationEndAt;
    }

    public void setRegistrationEndAt(Instant registrationEndAt) {
        this.registrationEndAt = registrationEndAt;
    }

    public Instant getCompetitionStartAt() {
        return competitionStartAt;
    }

    public void setCompetitionStartAt(Instant competitionStartAt) {
        this.competitionStartAt = competitionStartAt;
    }

    public Instant getCompetitionEndAt() {
        return competitionEndAt;
    }

    public void setCompetitionEndAt(Instant competitionEndAt) {
        this.competitionEndAt = competitionEndAt;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public Integer getBracketSize() {
        return bracketSize;
    }

    public void setBracketSize(Integer bracketSize) {
        this.bracketSize = bracketSize;
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
