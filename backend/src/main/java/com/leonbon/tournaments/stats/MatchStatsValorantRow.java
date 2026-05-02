package com.leonbon.tournaments.stats;

/** Estadísticas de un jugador en un partido (Valorant); solo métricas de la plataforma. */
public class MatchStatsValorantRow {
    private String userId;
    private Double kda;
    private Integer kills;
    private Integer deaths;
    private Integer assists;
    private Double headshotPct;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getKda() {
        return kda;
    }

    public void setKda(Double kda) {
        this.kda = kda;
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

    public Integer getAssists() {
        return assists;
    }

    public void setAssists(Integer assists) {
        this.assists = assists;
    }

    public Double getHeadshotPct() {
        return headshotPct;
    }

    public void setHeadshotPct(Double headshotPct) {
        this.headshotPct = headshotPct;
    }
}
