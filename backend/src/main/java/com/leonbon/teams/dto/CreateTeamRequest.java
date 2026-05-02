package com.leonbon.teams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CreateTeamRequest {
    @NotBlank
    @Size(min = 2, max = 64)
    private String name;

    @NotBlank
    @Size(min = 2, max = 8)
    private String tag;

    @NotBlank
    @Size(max = 64)
    private String regionServer;

    /** Opcional — patrocinio / partners (≤15 líneas cortas). */
    private List<
                    @Size(max = 200)
                    String>
            sponsorLines;

    @Size(max = 512)
    private String canonicalStreamUrl;

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

    public List<String> getSponsorLines() {
        return sponsorLines;
    }

    public void setSponsorLines(List<String> sponsorLines) {
        this.sponsorLines = sponsorLines;
    }

    public String getCanonicalStreamUrl() {
        return canonicalStreamUrl;
    }

    public void setCanonicalStreamUrl(String canonicalStreamUrl) {
        this.canonicalStreamUrl = canonicalStreamUrl;
    }
}
