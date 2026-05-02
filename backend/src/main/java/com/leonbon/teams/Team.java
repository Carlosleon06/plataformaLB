package com.leonbon.teams;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("teams")
public class Team {
    @Id
    private String id;

    @Indexed
    private String name;

    private String tag;

    private String regionServer;

    private String logoUrl;

    private TeamStatus status = TeamStatus.PENDING;

    private String captainUserId;

    private List<String> coachUserIds = new ArrayList<>();

    private List<String> memberUserIds = new ArrayList<>();

    /** Patrocinio / partners (texto libre corto por línea). */

    private List<String> sponsorLines = new ArrayList<>();

    /** Enlace destacado oficial del equipo (streaming u otra plataforma). */

    private String canonicalStreamUrl;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getRegionServer() {
        return regionServer;
    }

    public void setRegionServer(String regionServer) {
        this.regionServer = regionServer;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public TeamStatus getStatus() {
        return status;
    }

    public void setStatus(TeamStatus status) {
        this.status = status;
    }

    public String getCaptainUserId() {
        return captainUserId;
    }

    public void setCaptainUserId(String captainUserId) {
        this.captainUserId = captainUserId;
    }

    public List<String> getCoachUserIds() {
        return coachUserIds;
    }

    public void setCoachUserIds(List<String> coachUserIds) {
        this.coachUserIds = coachUserIds;
    }

    public List<String> getMemberUserIds() {
        return memberUserIds;
    }

    public void setMemberUserIds(List<String> memberUserIds) {
        this.memberUserIds = memberUserIds;
    }

    public List<String> getSponsorLines() {
        return sponsorLines;
    }

    public void setSponsorLines(List<String> sponsorLines) {
        this.sponsorLines = sponsorLines == null ? new ArrayList<>() : new ArrayList<>(sponsorLines);
    }

    public String getCanonicalStreamUrl() {
        return canonicalStreamUrl;
    }

    public void setCanonicalStreamUrl(String canonicalStreamUrl) {
        this.canonicalStreamUrl = canonicalStreamUrl;
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
