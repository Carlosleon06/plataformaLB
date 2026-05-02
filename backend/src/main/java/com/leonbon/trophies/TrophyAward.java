package com.leonbon.trophies;

import com.leonbon.tournaments.GameTitle;
import com.leonbon.tournaments.TournamentEntryType;
import com.leonbon.tournaments.TournamentFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Resultado publicable al cerrar un torneo (palmarés interno). */

@Document(collection = "trophy_awards")
public class TrophyAward {
    @Id
    private String id;

    @Indexed
    private String tournamentId;

    private String tournamentName;

    private GameTitle game;

    private TournamentFormat tournamentFormat;

    /** 1 campeón, 2 subcampeón, … */
    private int placement;

    private String badgeLabel;

    private String tournamentEntryId;

    private TournamentEntryType entryType;

    private String teamId;

    private String playerId;

    private List<String> creditedMemberUserIds = new ArrayList<>();

    private Instant awardedAt = Instant.now();

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

    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public GameTitle getGame() {
        return game;
    }

    public void setGame(GameTitle game) {
        this.game = game;
    }

    public TournamentFormat getTournamentFormat() {
        return tournamentFormat;
    }

    public void setTournamentFormat(TournamentFormat tournamentFormat) {
        this.tournamentFormat = tournamentFormat;
    }

    public int getPlacement() {
        return placement;
    }

    public void setPlacement(int placement) {
        this.placement = placement;
    }

    public String getBadgeLabel() {
        return badgeLabel;
    }

    public void setBadgeLabel(String badgeLabel) {
        this.badgeLabel = badgeLabel;
    }

    public String getTournamentEntryId() {
        return tournamentEntryId;
    }

    public void setTournamentEntryId(String tournamentEntryId) {
        this.tournamentEntryId = tournamentEntryId;
    }

    public TournamentEntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(TournamentEntryType entryType) {
        this.entryType = entryType;
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

    public List<String> getCreditedMemberUserIds() {
        return creditedMemberUserIds;
    }

    public void setCreditedMemberUserIds(List<String> creditedMemberUserIds) {
        this.creditedMemberUserIds =
                creditedMemberUserIds == null ? new ArrayList<>() : new ArrayList<>(creditedMemberUserIds);
    }

    public Instant getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(Instant awardedAt) {
        this.awardedAt = awardedAt;
    }
}
