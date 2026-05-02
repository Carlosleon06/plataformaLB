package com.leonbon.tournaments.dto;

import jakarta.validation.constraints.NotBlank;

public class SetMatchWinnerRequest {
    @NotBlank
    private String winnerEntryId;

    public String getWinnerEntryId() {
        return winnerEntryId;
    }

    public void setWinnerEntryId(String winnerEntryId) {
        this.winnerEntryId = winnerEntryId;
    }
}
