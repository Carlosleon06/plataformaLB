package com.leonbon.tournaments;

import java.time.Instant;

/** Reglas de apertura/cierre de inscripciones (manual, por fechas o tarea programada). */
public final class TournamentRegistrationLifecycle {
    private TournamentRegistrationLifecycle() {}

    public static boolean isRegistrationAcceptingEntries(Tournament t, Instant now) {
        if (t.getLifecycleStatus() != TournamentLifecycleStatus.REGISTRATION_OPEN) {
            return false;
        }
        Instant end = t.getRegistrationEndAt();
        if (end != null && now.isAfter(end)) {
            return false;
        }
        if (!t.isRegistrationManuallyOpened()) {
            Instant start = t.getRegistrationStartAt();
            if (start != null && now.isBefore(start)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Avanza el estado según fechas: programado → abierto al inicio; abierto (no manual) → cerrado al fin.
     *
     * @return true si el documento cambió y debe persistirse
     */
    public static boolean syncScheduledTransitions(Tournament t, Instant now) {
        Instant start = t.getRegistrationStartAt();
        Instant end = t.getRegistrationEndAt();
        if (start == null || end == null) {
            return false;
        }

        TournamentLifecycleStatus status = t.getLifecycleStatus();
        if (status == TournamentLifecycleStatus.REGISTRATION_SCHEDULED) {
            if (!now.isBefore(start) && !now.isAfter(end)) {
                t.setLifecycleStatus(TournamentLifecycleStatus.REGISTRATION_OPEN);
                t.setRegistrationManuallyOpened(false);
                t.setUpdatedAt(now);
                return true;
            }
            if (now.isAfter(end)) {
                t.setLifecycleStatus(TournamentLifecycleStatus.REGISTRATION_CLOSED);
                t.setUpdatedAt(now);
                return true;
            }
            return false;
        }

        if (status == TournamentLifecycleStatus.REGISTRATION_OPEN && now.isAfter(end)) {
            t.setLifecycleStatus(TournamentLifecycleStatus.REGISTRATION_CLOSED);
            t.setUpdatedAt(now);
            return true;
        }

        return false;
    }
}
