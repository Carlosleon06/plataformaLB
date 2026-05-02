package com.leonbon.tournaments.stats;

/** Estadísticas de un jugador en un partido (Fortnite); solo métricas de la plataforma. */
public class MatchStatsFortniteRow {
    private String userId;
    private Integer kills;
    private Integer deaths;
    /** Placement en la partida (1 = victoria típica en BR). */
    private Integer placement;
    private String modePlayed;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getKills() {
        return kills;
    }

    public void setKills(Integer kills) {
        this.kills = kills;
    }

    public Integer getDeaths() {
        return deaths;
    }

    public void setDeaths(Integer deaths) {
        this.deaths = deaths;
    }

    public Integer getPlacement() {
        return placement;
    }

    public void setPlacement(Integer placement) {
        this.placement = placement;
    }

    public String getModePlayed() {
        return modePlayed;
    }

    public void setModePlayed(String modePlayed) {
        this.modePlayed = modePlayed;
    }
}
