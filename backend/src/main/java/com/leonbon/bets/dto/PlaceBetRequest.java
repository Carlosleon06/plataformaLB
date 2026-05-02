package com.leonbon.bets.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class PlaceBetRequest {
    @NotBlank
    private String matchId;

    @NotBlank
    private String pickedEntryId;

    @Min(1)
    private long amount;

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getPickedEntryId() {
        return pickedEntryId;
    }

    public void setPickedEntryId(String pickedEntryId) {
        this.pickedEntryId = pickedEntryId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }
}

