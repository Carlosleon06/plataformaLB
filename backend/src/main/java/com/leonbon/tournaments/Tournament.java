package com.leonbon.tournaments;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    private TournamentLifecycleStatus lifecycleStatus = TournamentLifecycleStatus.REGISTRATION_SCHEDULED;

    /** true si el admin abrió inscripciones antes de registrationStartAt. */
    private boolean registrationManuallyOpened;

    private Instant registrationStartAt;
    private Instant registrationEndAt;

    private Instant competitionStartAt;
    private Instant competitionEndAt;

    private String streamUrl;

    /** Reglamento público del torneo (texto libre). */
    private String rulesHtml;

    /** Requisitos de elegibilidad; la verificación real es manual por admin. */
    private String eligibilityNotes;

    /** Descripción del premio / bolsa (texto libre). */
    private String prizeNotes;

    /** Null = sin tope explícito; admin no podrá sobrepasar al aprobar entradas. */
    private Integer maxApprovedParticipants;

    /** Power-of-two bracket size after generation; null before bracket exists. */
    private Integer bracketSize;

    /**
     * Cuántos puestos clasificados reciben L-Coins al cerrarse el torneo (1 = sólo campeón, …). Null/0 =
     * sin tabla monetaria configurada en creación (legacy).
     */
    private Integer prizeWinnerSlots;

    /** Lista ordenada índice 0 = campeón, tamaño igual a {@link #prizeWinnerSlots}; null en torneos legacy. */
    private List<Long> prizeLeonCoinsByPlacement;

    /** Marca tiempo tras liquidar una vez la tabla de premios en L-Coins (si aplica). */
    private Instant placementPrizeLedgerCompletedAt;

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

    public boolean isRegistrationManuallyOpened() {
        return registrationManuallyOpened;
    }

    public void setRegistrationManuallyOpened(boolean registrationManuallyOpened) {
        this.registrationManuallyOpened = registrationManuallyOpened;
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

    public String getRulesHtml() {
        return rulesHtml;
    }

    public void setRulesHtml(String rulesHtml) {
        this.rulesHtml = rulesHtml;
    }

    public String getEligibilityNotes() {
        return eligibilityNotes;
    }

    public void setEligibilityNotes(String eligibilityNotes) {
        this.eligibilityNotes = eligibilityNotes;
    }

    public String getPrizeNotes() {
        return prizeNotes;
    }

    public void setPrizeNotes(String prizeNotes) {
        this.prizeNotes = prizeNotes;
    }

    public Integer getMaxApprovedParticipants() {
        return maxApprovedParticipants;
    }

    public void setMaxApprovedParticipants(Integer maxApprovedParticipants) {
        this.maxApprovedParticipants = maxApprovedParticipants;
    }

    public Integer getBracketSize() {
        return bracketSize;
    }

    public void setBracketSize(Integer bracketSize) {
        this.bracketSize = bracketSize;
    }

    public Integer getPrizeWinnerSlots() {
        return prizeWinnerSlots;
    }

    public void setPrizeWinnerSlots(Integer prizeWinnerSlots) {
        this.prizeWinnerSlots = prizeWinnerSlots;
    }

    public List<Long> getPrizeLeonCoinsByPlacement() {
        return prizeLeonCoinsByPlacement;
    }

    public void setPrizeLeonCoinsByPlacement(List<Long> prizeLeonCoinsByPlacement) {
        this.prizeLeonCoinsByPlacement =
                prizeLeonCoinsByPlacement == null ? null : new ArrayList<>(prizeLeonCoinsByPlacement);
    }

    public Instant getPlacementPrizeLedgerCompletedAt() {
        return placementPrizeLedgerCompletedAt;
    }

    public void setPlacementPrizeLedgerCompletedAt(Instant placementPrizeLedgerCompletedAt) {
        this.placementPrizeLedgerCompletedAt = placementPrizeLedgerCompletedAt;
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
