package com.leonbon.tournaments;

public enum BracketMatchStatus {
    /** Upper rounds: waiting for both child matches to finish. */
    WAITING,
    /** Both slots known; admin can record winner (or auto bye if one side empty in upper round — not used in MVP). */
    READY,
    COMPLETE
}
