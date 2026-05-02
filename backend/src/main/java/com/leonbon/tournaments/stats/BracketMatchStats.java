package com.leonbon.tournaments.stats;

import com.leonbon.tournaments.GameTitle;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "bracket_match_stats")
public class BracketMatchStats {
    @Id
    private String id;

    @Indexed(unique = true)
    private String matchId;

    private String tournamentId;

    private GameTitle game;

    /** Usuario Mongo del admin que guardó última revisión */
    private String recordedByAdminUserId;

    private int revision;

    private Instant recordedAt;

    private List<MatchStatsValorantRow> valorantRows = new ArrayList<>();
    private List<MatchStatsFortniteRow> fortniteRows = new ArrayList<>();
    private List<MatchStatsMlbRow> mlbRows = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(String tournamentId) {
        this.tournamentId = tournamentId;
    }

    public GameTitle getGame() {
        return game;
    }

    public void setGame(GameTitle game) {
        this.game = game;
    }

    public String getRecordedByAdminUserId() {
        return recordedByAdminUserId;
    }

    public void setRecordedByAdminUserId(String recordedByAdminUserId) {
        this.recordedByAdminUserId = recordedByAdminUserId;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public List<MatchStatsValorantRow> getValorantRows() {
        return valorantRows;
    }

    public void setValorantRows(List<MatchStatsValorantRow> valorantRows) {
        this.valorantRows = valorantRows;
    }

    public List<MatchStatsFortniteRow> getFortniteRows() {
        return fortniteRows;
    }

    public void setFortniteRows(List<MatchStatsFortniteRow> fortniteRows) {
        this.fortniteRows = fortniteRows;
    }

    public List<MatchStatsMlbRow> getMlbRows() {
        return mlbRows;
    }

    public void setMlbRows(List<MatchStatsMlbRow> mlbRows) {
        this.mlbRows = mlbRows;
    }
}
