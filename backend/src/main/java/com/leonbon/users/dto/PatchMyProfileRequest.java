package com.leonbon.users.dto;

import com.leonbon.tournaments.GameTitle;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class PatchMyProfileRequest {
    @Size(max = 64)
    private String nickname;

    @Email
    @Size(max = 120)
    private String email;

    @Size(max = 120)
    private String fullName;

    /** null = sin cambios; omitir en cliente para no togglear. */

    private Boolean profileShowFullName;

    @Size(max = 64)
    private String country;

    @Size(max = 256)
    private String twitchProfileUrl;

    @Size(max = 256)
    private String youtubeChannelUrl;

    @Size(max = 256)
    private String xProfileUrl;

    @Size(max = 256)
    private String instagramProfileUrl;

    @Size(max = 80)
    private String discordHandle;

    private GameTitle preferredGame;

    /** Reemplaza el map completo cuando no es null (valores vacíos borran esa clave). */
    private Map<String, String> rankLabelsByGame;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Boolean getProfileShowFullName() {
        return profileShowFullName;
    }

    public void setProfileShowFullName(Boolean profileShowFullName) {
        this.profileShowFullName = profileShowFullName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getTwitchProfileUrl() {
        return twitchProfileUrl;
    }

    public void setTwitchProfileUrl(String twitchProfileUrl) {
        this.twitchProfileUrl = twitchProfileUrl;
    }

    public String getYoutubeChannelUrl() {
        return youtubeChannelUrl;
    }

    public void setYoutubeChannelUrl(String youtubeChannelUrl) {
        this.youtubeChannelUrl = youtubeChannelUrl;
    }

    public String getXProfileUrl() {
        return xProfileUrl;
    }

    public void setXProfileUrl(String xProfileUrl) {
        this.xProfileUrl = xProfileUrl;
    }

    public String getInstagramProfileUrl() {
        return instagramProfileUrl;
    }

    public void setInstagramProfileUrl(String instagramProfileUrl) {
        this.instagramProfileUrl = instagramProfileUrl;
    }

    public String getDiscordHandle() {
        return discordHandle;
    }

    public void setDiscordHandle(String discordHandle) {
        this.discordHandle = discordHandle;
    }

    public GameTitle getPreferredGame() {
        return preferredGame;
    }

    public void setPreferredGame(GameTitle preferredGame) {
        this.preferredGame = preferredGame;
    }

    public Map<String, String> getRankLabelsByGame() {
        return rankLabelsByGame;
    }

    public void setRankLabelsByGame(Map<String, String> rankLabelsByGame) {
        this.rankLabelsByGame = rankLabelsByGame;
    }
}
