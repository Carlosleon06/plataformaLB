package com.leonbon.tournaments.dto;

import com.leonbon.tournaments.GameTitle;
import com.leonbon.tournaments.TournamentFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class CreateTournamentRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String organizers;

    @NotNull
    private GameTitle game;

    @NotNull
    private TournamentFormat format;

    @NotNull
    private Instant registrationStartAt;

    @NotNull
    private Instant registrationEndAt;

    @NotNull
    private Instant competitionStartAt;

    @NotNull
    private Instant competitionEndAt;

    private String streamUrl;

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
}
