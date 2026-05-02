package com.leonbon.teams.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
}
