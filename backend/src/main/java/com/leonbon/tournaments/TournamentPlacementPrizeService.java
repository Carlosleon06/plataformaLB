package com.leonbon.tournaments;

import com.leonbon.economy.Transaction;
import com.leonbon.economy.TransactionRepository;
import com.leonbon.economy.TransactionType;
import com.leonbon.trophies.TrophyAward;
import com.leonbon.trophies.TrophyAwardRepository;
import com.leonbon.users.User;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class TournamentPlacementPrizeService {
    private final TournamentRepository tournamentRepository;
    private final TrophyAwardRepository trophyAwardRepository;
    private final MongoTemplate mongoTemplate;
    private final TransactionRepository transactionRepository;

    public TournamentPlacementPrizeService(
            TournamentRepository tournamentRepository,
            TrophyAwardRepository trophyAwardRepository,
            MongoTemplate mongoTemplate,
            TransactionRepository transactionRepository) {
        this.tournamentRepository = tournamentRepository;
        this.trophyAwardRepository = trophyAwardRepository;
        this.mongoTemplate = mongoTemplate;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Reparte L-Coins por colocación usando la tabla guardada en el torneo y los {@link TrophyAward} emitidos al cerrar.
     * Idempotente vía {@link Tournament#getPlacementPrizeLedgerCompletedAt()}.
     */
    public void payOutIfNeeded(String tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId).orElse(null);
        if (tournament == null || tournament.getLifecycleStatus() != TournamentLifecycleStatus.COMPLETED) {
            return;
        }
        if (tournament.getPlacementPrizeLedgerCompletedAt() != null) {
            return;
        }
        Integer slots = tournament.getPrizeWinnerSlots();
        List<Long> configured = tournament.getPrizeLeonCoinsByPlacement();
        boolean noTable =
                slots == null || slots <= 0 || configured == null || configured.isEmpty();

        Instant now = Instant.now();
        if (noTable) {
            return;
        }
        if (slots > 64) {
            return;
        }
        if (configured.size() != slots) {
            return;
        }
        boolean anyPositive = configured.stream().filter(Objects::nonNull).anyMatch(x -> x > 0);
        if (!anyPositive) {
            markLedgerDone(tournamentId, now);
            return;
        }

        if (!trophyAwardRepository.existsByTournamentId(tournamentId)) {
            return;
        }

        for (int p = 1; p <= slots; p++) {
            Long raw = configured.get(p - 1);
            long pool = raw == null ? 0L : raw;
            if (pool <= 0) {
                continue;
            }
            TrophyAward award =
                    trophyAwardRepository
                            .findFirstByTournamentIdAndPlacement(tournamentId, p)
                            .orElse(null);
            if (award == null) {
                continue;
            }
            List<String> beneficiaries = uniqueSortedUserIds(award);
            if (beneficiaries.isEmpty()) {
                continue;
            }
            splitAndCredit(tournamentId, award.getId(), beneficiaries, pool, now);
        }

        markLedgerDone(tournamentId, now);
    }

    private void markLedgerDone(String tournamentId, Instant now) {
        Tournament fresh = tournamentRepository.findById(tournamentId).orElse(null);
        if (fresh == null) {
            return;
        }
        fresh.setPlacementPrizeLedgerCompletedAt(now);
        fresh.setUpdatedAt(now);
        tournamentRepository.save(fresh);
    }

    private static List<String> uniqueSortedUserIds(TrophyAward award) {
        List<String> base = award.getCreditedMemberUserIds();
        if (base == null || base.isEmpty()) {
            return List.of();
        }
        return base.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    private void splitAndCredit(String tournamentId, String trophyAwardId, List<String> userIds, long pool, Instant now) {
        int n = userIds.size();
        long base = pool / n;
        long remainder = pool % n;
        for (int i = 0; i < n; i++) {
            long amt = base + (i < remainder ? 1L : 0L);
            if (amt <= 0) {
                continue;
            }
            String uid = userIds.get(i);
            creditLeonCoins(uid, amt, now, refKey(tournamentId, trophyAwardId, uid));
        }
    }

    private static String refKey(String tournamentId, String trophyAwardId, String userId) {
        return "tournament_prize:%s:%s:%s".formatted(tournamentId, trophyAwardId, userId);
    }

    private void creditLeonCoins(String userId, long amount, Instant now, String ref) {
        if (transactionRepository.findByRef(ref).isPresent()) {
            return;
        }
        Query q = new Query(Criteria.where("_id").is(userId));
        Update u = new Update().inc("leonCoinsBalance", amount).set("updatedAt", now);
        User updated =
                mongoTemplate.findAndModify(
                        q, u, FindAndModifyOptions.options().returnNew(true), User.class);
        if (updated == null) {
            return;
        }
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setType(TransactionType.TOURNAMENT_PLACEMENT_PRIZE);
        tx.setAmount(amount);
        tx.setBalanceAfter(updated.getLeonCoinsBalance());
        tx.setRef(ref);
        tx.setCreatedAt(now);
        transactionRepository.save(tx);
    }
}
