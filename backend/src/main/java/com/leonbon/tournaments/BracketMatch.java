package com.leonbon.tournaments;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("bracket_matches")
public class BracketMatch {
    @Id
    private String id;

    @Indexed
    private String tournamentId;

    /** 1 = first round (closest to seeds). */
    private int round;

    private int indexInRound;

    /** TournamentEntry ids participating in this slot (null = bye). */
    private String entryIdA;

    private String entryIdB;

    private String winnerEntryId;

    private BracketMatchStatus status = BracketMatchStatus.WAITING;

    /** Sub-bracket; null in legacy DB rows means {@link BracketPool#WB}. */
    private BracketPool bracketPool;

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

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public int getIndexInRound() {
        return indexInRound;
    }

    public void setIndexInRound(int indexInRound) {
        this.indexInRound = indexInRound;
    }

    public String getEntryIdA() {
        return entryIdA;
    }

    public void setEntryIdA(String entryIdA) {
        this.entryIdA = entryIdA;
    }

    public String getEntryIdB() {
        return entryIdB;
    }

    public void setEntryIdB(String entryIdB) {
        this.entryIdB = entryIdB;
    }

    public String getWinnerEntryId() {
        return winnerEntryId;
    }

    public void setWinnerEntryId(String winnerEntryId) {
        this.winnerEntryId = winnerEntryId;
    }

    public BracketMatchStatus getStatus() {
        return status;
    }

    public void setStatus(BracketMatchStatus status) {
        this.status = status;
    }

    public BracketPool getBracketPool() {
        return bracketPool == null ? BracketPool.WB : bracketPool;
    }

    public void setBracketPool(BracketPool bracketPool) {
        this.bracketPool = bracketPool;
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
