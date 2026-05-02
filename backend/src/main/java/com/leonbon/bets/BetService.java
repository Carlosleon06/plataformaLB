package com.leonbon.bets;

import com.leonbon.auth.ConflictException;
import com.leonbon.economy.Transaction;
import com.leonbon.economy.TransactionRepository;
import com.leonbon.economy.TransactionType;
import com.leonbon.realtime.RealtimeMessagingService;
import com.leonbon.tournaments.BracketMatch;
import com.leonbon.tournaments.BracketMatchRepository;
import com.leonbon.tournaments.BracketMatchStatus;
import com.leonbon.users.User;
import com.leonbon.web.BadRequestException;
import com.leonbon.web.NotFoundException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class BetService {
    /** Retorno estimado por 1 moneda apostada en ese bando si ese bando ganara (sin contar la propia apuesta futura). Parimutuel. */
    public record MatchStakeBoard(
            long stakeOnA,
            long stakeOnB,
            Double impliedReturnPerCoinOnA,
            Double impliedReturnPerCoinOnB,
            Instant bettingClosesAt,
            int bettingWindowMinutes
    ) {}

    private final MongoTemplate mongoTemplate;
    private final BetRepository betRepository;
    private final BracketMatchRepository bracketMatchRepository;
    private final TransactionRepository transactionRepository;
    private final BettingConfig bettingConfig;
    private final RealtimeMessagingService realtimeMessagingService;

    public BetService(
            MongoTemplate mongoTemplate,
            BetRepository betRepository,
            BracketMatchRepository bracketMatchRepository,
            TransactionRepository transactionRepository,
            BettingConfig bettingConfig,
            RealtimeMessagingService realtimeMessagingService
    ) {
        this.mongoTemplate = mongoTemplate;
        this.betRepository = betRepository;
        this.bracketMatchRepository = bracketMatchRepository;
        this.transactionRepository = transactionRepository;
        this.bettingConfig = bettingConfig;
        this.realtimeMessagingService = realtimeMessagingService;
    }

    public MatchStakeBoard stakeBoardForMatch(BracketMatch match) {
        List<Bet> pending = betRepository.findByMatchIdAndStatus(match.getId(), BetStatus.PENDING);
        long a = 0;
        long b = 0;
        String ea = match.getEntryIdA();
        for (Bet bet : pending) {
            if (Objects.equals(bet.getPickedEntryId(), ea)) {
                a += bet.getAmount();
            } else {
                b += bet.getAmount();
            }
        }
        int w = bettingConfig.windowMinutes();
        Instant now = Instant.now();
        Instant rawClose = match.getBettingWindowClosesAt();
        Instant closes = (rawClose != null && now.isBefore(rawClose)) ? rawClose : null;
        Double ma = a > 0 ? ((double) (a + b)) / (double) a : null;
        Double mb = b > 0 ? ((double) (a + b)) / (double) b : null;
        return new MatchStakeBoard(a, b, ma, mb, closes, w);
    }

    public Bet placeBet(String userId, String matchId, String pickedEntryId, long amount) {
        if (amount <= 0) throw new BadRequestException("amount must be >= 1");

        BracketMatch match = bracketMatchRepository.findById(matchId).orElseThrow(() -> new NotFoundException("match not found"));
        if (match.getStatus() != BracketMatchStatus.READY) {
            throw new ConflictException("betting is only allowed when match is READY");
        }
        if (match.getWinnerEntryId() != null) {
            throw new ConflictException("match already has a winner");
        }
        if (match.getEntryIdA() == null || match.getEntryIdB() == null) {
            throw new ConflictException("betting is not allowed for BYE/empty matches");
        }
        if (!Objects.equals(pickedEntryId, match.getEntryIdA()) && !Objects.equals(pickedEntryId, match.getEntryIdB())) {
            throw new BadRequestException("pickedEntryId must be one of the two entries");
        }

        Instant now = Instant.now();
        Instant closeUntil = match.getBettingWindowClosesAt();
        if (closeUntil == null || !now.isBefore(closeUntil)) {
            throw new ConflictException("betting window is closed");
        }

        // Deduct balance atomically
        Query q = new Query(new Criteria().andOperator(
                Criteria.where("_id").is(userId),
                Criteria.where("leonCoinsBalance").gte(amount)
        ));
        Update u = new Update()
                .inc("leonCoinsBalance", -amount)
                .set("updatedAt", now);
        User updated = mongoTemplate.findAndModify(q, u, FindAndModifyOptions.options().returnNew(true), User.class);
        if (updated == null) {
            throw new ConflictException("insufficient balance");
        }

        Bet bet = new Bet();
        bet.setUserId(userId);
        bet.setTournamentId(match.getTournamentId());
        bet.setMatchId(match.getId());
        bet.setPickedEntryId(pickedEntryId);
        bet.setAmount(amount);
        bet.setStatus(BetStatus.PENDING);
        bet.setCreatedAt(now);

        Bet saved;
        try {
            saved = betRepository.save(bet);
        } catch (DuplicateKeyException e) {
            refundBalance(userId, amount, "duplicate bet for match");
            throw new ConflictException("you already placed a bet for this match");
        }

        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setType(TransactionType.BET_PLACED);
        tx.setAmount(-amount);
        tx.setBalanceAfter(updated.getLeonCoinsBalance());
        tx.setRef("bet:" + saved.getId());
        tx.setCreatedAt(now);
        transactionRepository.save(tx);

        MatchStakeBoard boardAfter = stakeBoardForMatch(match);
        realtimeMessagingService.publishMatchBetBoard(match, boardAfter);

        return saved;
    }

    public void publishStakeBoard(BracketMatch match) {
        realtimeMessagingService.publishMatchBetBoard(match, stakeBoardForMatch(match));
    }

    /**
     * Parimutuel: el pozo ({@code(winnersStakes + losersStakes)}) se reparte entre apuestas ganadoras en proporción a su participación en el lado ganador.
     * Sobras por redondeo se reparten 1 LC a la vez favoreciendo las apuestas de mayor tamaño en el lado ganador.
     */
    public void resolveMatchBets(BracketMatch match, Instant now) {
        String winnerEntryId = match.getWinnerEntryId();
        if (winnerEntryId == null) return;

        List<Bet> pending = betRepository.findByMatchIdAndStatus(match.getId(), BetStatus.PENDING);
        if (pending.isEmpty()) return;

        String entryA = match.getEntryIdA();
        boolean aWins = Objects.equals(winnerEntryId, entryA);

        long stakeOnWinnerSide = 0;
        List<Bet> winnerBets = new ArrayList<>();
        for (Bet b : pending) {
            boolean onWinner = Objects.equals(b.getPickedEntryId(), winnerEntryId);
            if (onWinner) {
                stakeOnWinnerSide += b.getAmount();
                winnerBets.add(b);
            }
        }

        if (stakeOnWinnerSide == 0) {
            for (Bet b : pending) {
                refundBetFull(b, now, "no stakes on winner");
            }
            realtimeMessagingService.publishMatchBetBoard(match, stakeBoardForMatch(match));
            return;
        }

        long totalPot = 0;
        for (Bet b : pending) {
            totalPot += b.getAmount();
        }

        winnerBets.sort(Comparator.comparing(Bet::getAmount).reversed().thenComparing(Bet::getId));

        int nWin = winnerBets.size();
        long[] payouts = new long[nWin];
        long distributed = 0;
        for (int i = 0; i < nWin; i++) {
            long stake = winnerBets.get(i).getAmount();
            payouts[i] = mulDivFloor(stake, totalPot, stakeOnWinnerSide);
            distributed += payouts[i];
        }

        long remainder = totalPot - distributed;
        if (remainder > 0) {
            distributeRemainderRoundRobinDescending(winnerBets, payouts, remainder);
        } else if (remainder < 0) {
            throw new IllegalStateException("parimutuel rounding error: payouts exceed pool");
        }

        for (int i = 0; i < nWin; i++) {
            payOutWinningBet(winnerBets.get(i), payouts[i], now);
        }

        for (Bet b : pending) {
            if (!Objects.equals(b.getPickedEntryId(), winnerEntryId)) {
                b.setStatus(BetStatus.LOST);
                b.setPayoutAmount(0L);
                b.setResolvedAt(now);
                betRepository.save(b);
            }
        }

        realtimeMessagingService.publishMatchBetBoard(match, stakeBoardForMatch(match));
    }

    private static void distributeRemainderRoundRobinDescending(List<Bet> winnerBets, long[] payouts, long remainder) {
        int n = winnerBets.size();
        for (int k = 0; k < remainder; k++) {
            payouts[k % n]++;
        }
    }

    private void payOutWinningBet(Bet bet, long payout, Instant now) {
        Query q = new Query(Criteria.where("_id").is(bet.getUserId()));
        Update u = new Update()
                .inc("leonCoinsBalance", payout)
                .set("updatedAt", now);
        User updated = mongoTemplate.findAndModify(q, u, FindAndModifyOptions.options().returnNew(true), User.class);
        if (updated == null) throw new NotFoundException("user not found");

        bet.setStatus(BetStatus.WON);
        bet.setPayoutAmount(payout);
        bet.setResolvedAt(now);
        betRepository.save(bet);

        Transaction tx = new Transaction();
        tx.setUserId(bet.getUserId());
        tx.setType(TransactionType.BET_PAYOUT);
        tx.setAmount(payout);
        tx.setBalanceAfter(updated.getLeonCoinsBalance());
        tx.setRef("bet:" + bet.getId());
        tx.setCreatedAt(now);
        transactionRepository.save(tx);
    }

    private void refundBetFull(Bet bet, Instant now, String refPrefix) {
        refundBalance(bet.getUserId(), bet.getAmount(), refPrefix + ":bet:" + bet.getId());

        bet.setStatus(BetStatus.REFUNDED);
        bet.setPayoutAmount(0L);
        bet.setResolvedAt(now);
        betRepository.save(bet);
    }

    private void refundBalance(String userId, long amount, String ref) {
        Instant n = Instant.now();
        Query q = new Query(Criteria.where("_id").is(userId));
        Update u = new Update()
                .inc("leonCoinsBalance", amount)
                .set("updatedAt", n);
        User updated = mongoTemplate.findAndModify(q, u, FindAndModifyOptions.options().returnNew(true), User.class);
        if (updated == null) return;
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setType(TransactionType.BET_REFUND);
        tx.setAmount(amount);
        tx.setBalanceAfter(updated.getLeonCoinsBalance());
        tx.setRef(ref);
        tx.setCreatedAt(n);
        transactionRepository.save(tx);
    }

    private static long mulDivFloor(long stake, long numerator, long denominator) {
        if (denominator <= 0) {
            throw new BadRequestException("invalid pool distribution");
        }
        return BigInteger.valueOf(stake)
                .multiply(BigInteger.valueOf(numerator))
                .divide(BigInteger.valueOf(denominator))
                .longValueExact();
    }
}
