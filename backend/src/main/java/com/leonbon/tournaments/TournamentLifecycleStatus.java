package com.leonbon.tournaments;

public enum TournamentLifecycleStatus {
    /** Creado; inscripción abre en registrationStartAt o cuando el admin la abre manualmente. */
    REGISTRATION_SCHEDULED,
    REGISTRATION_OPEN,
    REGISTRATION_CLOSED,
    LIVE,
    COMPLETED
}
