package com.leonbon.tournaments.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Exactamente uno de los tres debe ser no-null por petición al servicio (validación aplicada ahí). */
public class UpsertBracketMatchStatsRequest {

    @Valid
    private List<ValorantPlayerIn> valorantPlayers;

    @Valid
    private List<FortnitePlayerIn> fortnitePlayers;

    @Valid
    private List<MlbPlayerIn> mlbPlayers;

    public List<ValorantPlayerIn> getValorantPlayers() {
        return valorantPlayers;
    }

    public void setValorantPlayers(List<ValorantPlayerIn> valorantPlayers) {
        this.valorantPlayers = valorantPlayers;
    }

    public List<FortnitePlayerIn> getFortnitePlayers() {
        return fortnitePlayers;
    }

    public void setFortnitePlayers(List<FortnitePlayerIn> fortnitePlayers) {
        this.fortnitePlayers = fortnitePlayers;
    }

    public List<MlbPlayerIn> getMlbPlayers() {
        return mlbPlayers;
    }

    public void setMlbPlayers(List<MlbPlayerIn> mlbPlayers) {
        this.mlbPlayers = mlbPlayers;
    }

    public static class ValorantPlayerIn {
        @NotBlank
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

    public static class FortnitePlayerIn {
        @NotBlank
        private String userId;

        private Integer kills;
        private Integer deaths;
        private Integer placement;

        @Size(max = 64)
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

    public static class MlbPlayerIn {
        @NotBlank
        private String userId;

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
}
