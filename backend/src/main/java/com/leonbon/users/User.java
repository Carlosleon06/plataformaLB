package com.leonbon.users;

import com.leonbon.tournaments.GameTitle;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    /** Id numérico legible (incremental); null en cuentas legacy hasta primera asignación. */
    private Long leonPlayerNumber;

    @Indexed(unique = true, sparse = true)
    private String emailNormalized;

    private String passwordHash;

    private String nickname;

    /** Nombre real opcional (visibilidad vía profileShowFullName). */
    private String fullName;

    private boolean profileShowFullName;

    /** Residencia o país opcional para el perfil público. */
    private String country;

    /** Enlaces públicos opcionales. */
    private String twitchProfileUrl;

    private String youtubeChannelUrl;
    private String xProfileUrl;
    private String instagramProfileUrl;

    /** Texto tipo handle o invito a Discord (no necesariamente URL). */
    private String discordHandle;

    /** Juego principal destacado en el perfil. */
    private GameTitle preferredGame;

    /** Rango o división textual por videojuego; clave típica: VALORANT, FORTNITE, MLB. */
    private Map<String, String> rankLabelByGame = new LinkedHashMap<>();

    private UserStatus status = UserStatus.ACTIVE;

    private UserRole role = UserRole.PLAYER;

    private long leonCoinsBalance = 0;

    private Instant lastDailyClaimAt;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Long getLeonPlayerNumber() {
        return leonPlayerNumber;
    }

    public void setLeonPlayerNumber(Long leonPlayerNumber) {
        this.leonPlayerNumber = leonPlayerNumber;
    }

    public String getEmailNormalized() {
        return emailNormalized;
    }

    public void setEmailNormalized(String emailNormalized) {
        this.emailNormalized = emailNormalized;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isProfileShowFullName() {
        return profileShowFullName;
    }

    public void setProfileShowFullName(boolean profileShowFullName) {
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

    public Map<String, String> getRankLabelByGame() {
        return rankLabelByGame;
    }

    public void setRankLabelByGame(Map<String, String> rankLabelByGame) {
        this.rankLabelByGame =
                rankLabelByGame == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rankLabelByGame);
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public long getLeonCoinsBalance() {
        return leonCoinsBalance;
    }

    public void setLeonCoinsBalance(long leonCoinsBalance) {
        this.leonCoinsBalance = leonCoinsBalance;
    }

    public Instant getLastDailyClaimAt() {
        return lastDailyClaimAt;
    }

    public void setLastDailyClaimAt(Instant lastDailyClaimAt) {
        this.lastDailyClaimAt = lastDailyClaimAt;
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

