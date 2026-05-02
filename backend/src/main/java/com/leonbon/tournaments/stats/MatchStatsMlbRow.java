package com.leonbon.tournaments.stats;

/** Estadísticas 1v1 MLB The Show tras un partido (bateo + pitcheo en la misma fila). */
public class MatchStatsMlbRow {
    private String userId;
    /** AVG del encuentro */
    private Double battingAvgGame;
    private Integer homeRunsGame;
    private Double inningsPitchedGame;
    private Double eraGame;
    private Integer runsAllowedGame;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getBattingAvgGame() {
        return battingAvgGame;
    }

    public void setBattingAvgGame(Double battingAvgGame) {
        this.battingAvgGame = battingAvgGame;
    }

    public Integer getHomeRunsGame() {
        return homeRunsGame;
    }

    public void setHomeRunsGame(Integer homeRunsGame) {
        this.homeRunsGame = homeRunsGame;
    }

    public Double getInningsPitchedGame() {
        return inningsPitchedGame;
    }

    public void setInningsPitchedGame(Double inningsPitchedGame) {
        this.inningsPitchedGame = inningsPitchedGame;
    }

    public Double getEraGame() {
        return eraGame;
    }

    public void setEraGame(Double eraGame) {
        this.eraGame = eraGame;
    }

    public Integer getRunsAllowedGame() {
        return runsAllowedGame;
    }

    public void setRunsAllowedGame(Integer runsAllowedGame) {
        this.runsAllowedGame = runsAllowedGame;
    }
}
