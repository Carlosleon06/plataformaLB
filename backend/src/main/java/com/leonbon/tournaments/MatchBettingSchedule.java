package com.leonbon.tournaments;

import java.time.Duration;
import java.time.Instant;

/** Asigna {@code scheduledStartAt} escalonado (orden sugerido de partidas). Las apuestas las abre el admin manualmente. */
public final class MatchBettingSchedule {
    private MatchBettingSchedule() {}

    /**
     * Índice estable por partido para escalonar horarios desde el inicio oficial de competencia.
     * Evita colisiones entre pools distintos usando prefijos grandes.
     */
    public static long slotIndexFor(BracketPool pool, int round, int indexInRound) {
        return switch (pool) {
            case RR -> (long) indexInRound;
            case WB -> (round <= 1 ? 0L : 5_000L) + (long) round * 97L + indexInRound;
            case LB -> 20_000L + (long) round * 131L + indexInRound;
            case GF -> 30_000L;
        };
    }

    public static Instant scheduledStartForSlot(Tournament tournament, long slotIndex, int staggerMinutes, Instant now) {
        Instant base = tournament.getCompetitionStartAt();
        long minutes = Math.multiplyExact(slotIndex, staggerMinutes);
        Instant candidate = base.plus(Duration.ofMinutes(minutes));
        /* Partidos que quedan READY en caliente (p. ej. rondas siguientes) no pueden quedar en el pasado. */
        return candidate.isBefore(now) ? now : candidate;
    }
}
