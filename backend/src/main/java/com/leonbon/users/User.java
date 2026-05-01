package com.leonbon.users;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String passwordHash;

    private String nickname;

    private UserStatus status = UserStatus.ACTIVE;

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

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
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

