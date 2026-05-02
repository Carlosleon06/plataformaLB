package com.leonbon.teams.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public class PatchCaptainTeamPresenceRequest {

    /** Líneas libres tipo «Patrocinio X» (máx. 15). */

    private List<
                    @Size(max = 200)
                    String>
            sponsorLines;

    @Size(max = 512)
    private String canonicalStreamUrl;

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
