package com.leonbon.tournaments;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TournamentRegistrationLifecycleTask {
    private static final Logger log = LoggerFactory.getLogger(TournamentRegistrationLifecycleTask.class);

    private final TournamentRepository tournamentRepository;

    public TournamentRegistrationLifecycleTask(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    @Scheduled(fixedDelayString = "${app.tournaments.registrationLifecycleSweepMs:60000}")
    public void sweep() {
        Instant now = Instant.now();
        List<Tournament> candidates =
                tournamentRepository.findByLifecycleStatusIn(
                        List.of(
                                TournamentLifecycleStatus.REGISTRATION_SCHEDULED,
                                TournamentLifecycleStatus.REGISTRATION_OPEN));
        int updated = 0;
        for (Tournament t : candidates) {
            if (TournamentRegistrationLifecycle.syncScheduledTransitions(t, now)) {
                tournamentRepository.save(t);
                updated++;
            }
        }
        if (updated > 0) {
            log.debug("[registration lifecycle] Updated {} tournament(s)", updated);
        }
    }
}
