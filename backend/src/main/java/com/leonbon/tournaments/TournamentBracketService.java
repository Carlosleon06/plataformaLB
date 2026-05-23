package com.leonbon.tournaments;

import com.leonbon.auth.ConflictException;
import com.leonbon.auth.JwtPrincipal;
import com.leonbon.bets.BettingConfig;
import com.leonbon.bets.BetService;
import com.leonbon.trophies.TrophyAwardIssuanceService;
import com.leonbon.tournaments.dto.BracketMatchResponse;
import com.leonbon.tournaments.dto.TournamentResponse;
import com.leonbon.users.User;
import com.leonbon.users.UserRepository;
import com.leonbon.users.UserRole;
import com.leonbon.web.BadRequestException;
import com.leonbon.web.ForbiddenException;
import com.leonbon.web.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TournamentBracketService {
    private final UserRepository userRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final BracketMatchRepository bracketMatchRepository;
    private final BetService betService;
    private final BettingConfig bettingConfig;
    private final TrophyAwardIssuanceService trophyAwardIssuanceService;

    public TournamentBracketService(
            UserRepository userRepository,
            TournamentRepository tournamentRepository,
            TournamentEntryRepository tournamentEntryRepository,
            BracketMatchRepository bracketMatchRepository,
            BetService betService,
            BettingConfig bettingConfig,
            TrophyAwardIssuanceService trophyAwardIssuanceService
    ) {
        this.userRepository = userRepository;
        this.tournamentRepository = tournamentRepository;
        this.tournamentEntryRepository = tournamentEntryRepository;
        this.bracketMatchRepository = bracketMatchRepository;
        this.betService = betService;
        this.bettingConfig = bettingConfig;
        this.trophyAwardIssuanceService = trophyAwardIssuanceService;
    }

    public TournamentResponse closeRegistrationAsAdmin(JwtPrincipal admin, String tournamentId) {
        assertDbAdmin(admin);
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        if (t.getLifecycleStatus() != TournamentLifecycleStatus.REGISTRATION_OPEN) {
            throw new ConflictException("tournament registration is not open");
        }
        Instant now = Instant.now();
        t.setLifecycleStatus(TournamentLifecycleStatus.REGISTRATION_CLOSED);
        t.setUpdatedAt(now);
        tournamentRepository.save(t);
        return toTournamentResponse(tournamentRepository.findById(tournamentId).orElseThrow());
    }

    /**
     * Abre inscripciones antes de {@code registrationStartAt} (solo admin, torneo en REGISTRATION_SCHEDULED).
     */
    public TournamentResponse openRegistrationAsAdmin(JwtPrincipal admin, String tournamentId) {
        assertDbAdmin(admin);
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        if (t.getLifecycleStatus() != TournamentLifecycleStatus.REGISTRATION_SCHEDULED) {
            throw new ConflictException("solo se puede abrir inscripciones si el torneo está programado (REGISTRATION_SCHEDULED)");
        }
        assertNoBracketYet(tournamentId, t);
        Instant now = Instant.now();
        if (t.getRegistrationEndAt() != null && now.isAfter(t.getRegistrationEndAt())) {
            throw new ConflictException("la ventana de inscripción ya terminó; ajusta las fechas o crea otro torneo");
        }
        t.setLifecycleStatus(TournamentLifecycleStatus.REGISTRATION_OPEN);
        t.setRegistrationManuallyOpened(true);
        t.setUpdatedAt(now);
        tournamentRepository.save(t);
        return toTournamentResponse(tournamentRepository.findById(tournamentId).orElseThrow());
    }

    /**
     * Undo an accidental close-registration: back to {@link TournamentLifecycleStatus#REGISTRATION_OPEN} only when no
     * bracket has been generated yet (no matches, no bracketSize).
     */
    public TournamentResponse reopenRegistrationAsAdmin(JwtPrincipal admin, String tournamentId) {
        assertDbAdmin(admin);
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        if (t.getLifecycleStatus() != TournamentLifecycleStatus.REGISTRATION_CLOSED) {
            throw new ConflictException("solo se puede reabrir inscripciones si el estado es REGISTRATION_CLOSED");
        }
        assertNoBracketYet(tournamentId, t);
        Instant now = Instant.now();
        if (t.getRegistrationEndAt() != null && now.isAfter(t.getRegistrationEndAt())) {
            throw new ConflictException("la ventana de inscripción ya terminó");
        }
        t.setLifecycleStatus(TournamentLifecycleStatus.REGISTRATION_OPEN);
        t.setRegistrationManuallyOpened(true);
        t.setUpdatedAt(now);
        tournamentRepository.save(t);
        return toTournamentResponse(tournamentRepository.findById(tournamentId).orElseThrow());
    }

    private void assertNoBracketYet(String tournamentId, Tournament t) {
        if (bracketMatchRepository.countByTournamentId(tournamentId) > 0) {
            throw new ConflictException("no se puede modificar inscripciones: ya existen partidas de calendario");
        }
        if (t.getBracketSize() != null && t.getBracketSize() > 0) {
            throw new ConflictException("no se puede modificar inscripciones: el torneo ya tiene bracket generado (bracketSize)");
        }
    }

    /**
     * Removes a tournament and its entries when there is no bracket yet (no matches). Intended for admin mistakes
     * (e.g. empty tournament after closing registration).
     */
    public void deleteTournamentAsAdmin(JwtPrincipal admin, String tournamentId) {
        assertDbAdmin(admin);
        tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        if (bracketMatchRepository.countByTournamentId(tournamentId) > 0) {
            throw new ConflictException("no se puede eliminar: hay partidas de calendario");
        }
        tournamentEntryRepository.deleteByTournamentId(tournamentId);
        tournamentRepository.deleteById(tournamentId);
    }

    public TournamentResponse generateBracketAsAdmin(JwtPrincipal admin, String tournamentId) {
        assertDbAdmin(admin);
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        if (t.getLifecycleStatus() != TournamentLifecycleStatus.REGISTRATION_CLOSED) {
            throw new ConflictException("close registration before generating the bracket");
        }
        if (bracketMatchRepository.countByTournamentId(tournamentId) > 0) {
            throw new ConflictException("bracket already exists for this tournament");
        }

        List<TournamentEntry> approved =
                tournamentEntryRepository.findByTournamentIdAndStatusOrderByCreatedAtAsc(tournamentId, TournamentEntryStatus.APPROVED);
        if (approved.size() < 2) {
            throw new BadRequestException("need at least two approved entries");
        }

        return switch (t.getFormat()) {
            case SINGLE_ELIM -> generateSingleElimBracket(t, approved);
            case ROUND_ROBIN -> generateRoundRobinBracket(t, approved);
            case DOUBLE_ELIM -> generateDoubleElimBracket(t, approved);
        };
    }

    private TournamentResponse generateSingleElimBracket(Tournament t, List<TournamentEntry> approved) {
        int n = approved.size();
        int m = BracketMath.nextPow2(n);
        int totalRounds = BracketMath.roundsForBracketSize(m);

        List<String> padded = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            padded.add(i < approved.size() ? approved.get(i).getId() : null);
        }

        Instant now = Instant.now();
        t.setBracketSize(m);
        t.setUpdatedAt(now);
        tournamentRepository.save(t);

        int round1Matches = m / 2;
        for (int i = 0; i < round1Matches; i++) {
            BracketMatch bm = new BracketMatch();
            bm.setTournamentId(t.getId());
            bm.setBracketPool(BracketPool.WB);
            bm.setRound(1);
            bm.setIndexInRound(i);
            String a = padded.get(2 * i);
            String b = padded.get(2 * i + 1);
            bm.setEntryIdA(a);
            bm.setEntryIdB(b);
            applyByeOrReady(bm, now);
            bm.setCreatedAt(now);
            bm.setUpdatedAt(now);
            bracketMatchRepository.save(bm);
        }

        for (int r = 2; r <= totalRounds; r++) {
            int count = m >> r;
            for (int i = 0; i < count; i++) {
                BracketMatch bm = new BracketMatch();
                bm.setTournamentId(t.getId());
                bm.setBracketPool(BracketPool.WB);
                bm.setRound(r);
                bm.setIndexInRound(i);
                bm.setEntryIdA(null);
                bm.setEntryIdB(null);
                bm.setStatus(BracketMatchStatus.WAITING);
                bm.setCreatedAt(now);
                bm.setUpdatedAt(now);
                bracketMatchRepository.save(bm);
            }
        }

        for (int i = 0; i < round1Matches; i++) {
            BracketMatch child = loadMatch(t.getId(), BracketPool.WB, 1, i).orElseThrow();
            if (child.getStatus() == BracketMatchStatus.COMPLETE) {
                maybeAdvanceWinnersParent(t.getId(), child, totalRounds, TournamentFormat.SINGLE_ELIM);
            }
        }

        return goLive(t.getId());
    }

    private TournamentResponse generateRoundRobinBracket(Tournament t, List<TournamentEntry> approved) {
        Instant now = Instant.now();
        t.setBracketSize(approved.size());
        t.setUpdatedAt(now);
        tournamentRepository.save(t);

        int n = approved.size();
        int idx = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                BracketMatch bm = new BracketMatch();
                bm.setTournamentId(t.getId());
                bm.setBracketPool(BracketPool.RR);
                bm.setRound(1);
                bm.setIndexInRound(idx++);
                bm.setEntryIdA(approved.get(i).getId());
                bm.setEntryIdB(approved.get(j).getId());
                bm.setStatus(BracketMatchStatus.READY);
                stampBettingSchedule(bm, now);
                bm.setCreatedAt(now);
                bm.setUpdatedAt(now);
                bracketMatchRepository.save(bm);
            }
        }
        return goLive(t.getId());
    }

    /**
     * Standard double elimination for power-of-two field sizes {@code m} in 2, 4, 8 (with byes allowed in WB round 1).
     */
    private TournamentResponse generateDoubleElimBracket(Tournament t, List<TournamentEntry> approved) {
        int n = approved.size();
        int m = BracketMath.nextPow2(n);
        if (m > 8) {
            throw new BadRequestException("double elimination is supported for up to 8 bracket slots (power of two) in this version");
        }
        int wbRounds = BracketMath.roundsForBracketSize(m);

        List<String> padded = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            padded.add(i < approved.size() ? approved.get(i).getId() : null);
        }

        Instant now = Instant.now();
        t.setBracketSize(m);
        t.setUpdatedAt(now);
        tournamentRepository.save(t);

        // --- Winners bracket (same geometry as single elim) ---
        int round1Matches = m / 2;
        for (int i = 0; i < round1Matches; i++) {
            BracketMatch bm = new BracketMatch();
            bm.setTournamentId(t.getId());
            bm.setBracketPool(BracketPool.WB);
            bm.setRound(1);
            bm.setIndexInRound(i);
            bm.setEntryIdA(padded.get(2 * i));
            bm.setEntryIdB(padded.get(2 * i + 1));
            applyByeOrReady(bm, now);
            bm.setCreatedAt(now);
            bm.setUpdatedAt(now);
            bracketMatchRepository.save(bm);
        }
        for (int r = 2; r <= wbRounds; r++) {
            int count = m >> r;
            for (int i = 0; i < count; i++) {
                BracketMatch bm = new BracketMatch();
                bm.setTournamentId(t.getId());
                bm.setBracketPool(BracketPool.WB);
                bm.setRound(r);
                bm.setIndexInRound(i);
                bm.setEntryIdA(null);
                bm.setEntryIdB(null);
                bm.setStatus(BracketMatchStatus.WAITING);
                bm.setCreatedAt(now);
                bm.setUpdatedAt(now);
                bracketMatchRepository.save(bm);
            }
        }

        // --- Losers + grand final shells ---
        if (m == 2) {
            BracketMatch gf = new BracketMatch();
            gf.setTournamentId(t.getId());
            gf.setBracketPool(BracketPool.GF);
            gf.setRound(1);
            gf.setIndexInRound(0);
            gf.setEntryIdA(null);
            gf.setEntryIdB(null);
            gf.setStatus(BracketMatchStatus.WAITING);
            gf.setCreatedAt(now);
            gf.setUpdatedAt(now);
            bracketMatchRepository.save(gf);
        } else if (m == 4) {
            for (int[] spec : new int[][] {{1, 0}, {2, 0}}) {
                BracketMatch lb = emptyLb(t.getId(), spec[0], spec[1], now);
                bracketMatchRepository.save(lb);
            }
            bracketMatchRepository.save(emptyGf(t.getId(), now));
        } else if (m == 8) {
            for (int[] spec : new int[][] {{1, 0}, {1, 1}, {2, 0}, {2, 1}, {3, 0}, {4, 0}}) {
                bracketMatchRepository.save(emptyLb(t.getId(), spec[0], spec[1], now));
            }
            bracketMatchRepository.save(emptyGf(t.getId(), now));
        }

        for (int i = 0; i < round1Matches; i++) {
            BracketMatch child = loadMatch(t.getId(), BracketPool.WB, 1, i).orElseThrow();
            if (child.getStatus() == BracketMatchStatus.COMPLETE) {
                maybeAdvanceWinnersParent(t.getId(), child, wbRounds, TournamentFormat.DOUBLE_ELIM);
                routeDoubleElimLoserFromWinnersMatch(t.getId(), m, child);
            }
        }

        seedDoubleElimGrandFinalIfReady(t.getId(), m);

        return goLive(t.getId());
    }

    private static BracketMatch emptyLb(String tournamentId, int round, int index, Instant now) {
        BracketMatch lb = new BracketMatch();
        lb.setTournamentId(tournamentId);
        lb.setBracketPool(BracketPool.LB);
        lb.setRound(round);
        lb.setIndexInRound(index);
        lb.setEntryIdA(null);
        lb.setEntryIdB(null);
        lb.setStatus(BracketMatchStatus.WAITING);
        lb.setCreatedAt(now);
        lb.setUpdatedAt(now);
        return lb;
    }

    private static BracketMatch emptyGf(String tournamentId, Instant now) {
        BracketMatch gf = new BracketMatch();
        gf.setTournamentId(tournamentId);
        gf.setBracketPool(BracketPool.GF);
        gf.setRound(1);
        gf.setIndexInRound(0);
        gf.setEntryIdA(null);
        gf.setEntryIdB(null);
        gf.setStatus(BracketMatchStatus.WAITING);
        gf.setCreatedAt(now);
        gf.setUpdatedAt(now);
        return gf;
    }

    private TournamentResponse goLive(String tournamentId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        t.setLifecycleStatus(TournamentLifecycleStatus.LIVE);
        t.setUpdatedAt(Instant.now());
        tournamentRepository.save(t);
        return toTournamentResponse(tournamentRepository.findById(tournamentId).orElseThrow());
    }

    public List<BracketMatchResponse> listBracketMatches(String tournamentId) {
        tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        return bracketMatchRepository.findByTournamentIdOrderByBracketPoolAscRoundAscIndexInRoundAsc(tournamentId).stream()
                .map(this::toMatchResponse)
                .toList();
    }

    public BracketMatchResponse openBettingWindowAsAdmin(JwtPrincipal admin, String tournamentId, String matchId) {
        assertDbAdmin(admin);
        BracketMatch match = bracketMatchRepository.findById(matchId).orElseThrow(() -> new NotFoundException("match not found"));
        if (!Objects.equals(match.getTournamentId(), tournamentId)) {
            throw new NotFoundException("match not found");
        }
        if (match.getStatus() != BracketMatchStatus.READY) {
            throw new ConflictException("betting window only for READY matches");
        }
        if (match.getEntryIdA() == null || match.getEntryIdB() == null || match.getWinnerEntryId() != null) {
            throw new ConflictException("match is not open for betting");
        }
        Instant now = Instant.now();
        Instant closes = now.plus(Duration.ofMinutes(bettingConfig.windowMinutes()));
        match.setBettingWindowClosesAt(closes);
        match.setUpdatedAt(now);
        bracketMatchRepository.save(match);
        betService.publishStakeBoard(match);
        return toMatchResponse(bracketMatchRepository.findById(matchId).orElseThrow());
    }

    public BracketMatchResponse closeBettingWindowAsAdmin(JwtPrincipal admin, String tournamentId, String matchId) {
        assertDbAdmin(admin);
        BracketMatch match = bracketMatchRepository.findById(matchId).orElseThrow(() -> new NotFoundException("match not found"));
        if (!Objects.equals(match.getTournamentId(), tournamentId)) {
            throw new NotFoundException("match not found");
        }
        Instant now = Instant.now();
        match.setBettingWindowClosesAt(null);
        match.setUpdatedAt(now);
        bracketMatchRepository.save(match);
        betService.publishStakeBoard(match);
        return toMatchResponse(bracketMatchRepository.findById(matchId).orElseThrow());
    }

    public BracketMatchResponse setMatchWinnerAsAdmin(JwtPrincipal admin, String tournamentId, String matchId, String winnerEntryId) {
        assertDbAdmin(admin);
        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        BracketMatch match = bracketMatchRepository.findById(matchId).orElseThrow(() -> new NotFoundException("match not found"));
        if (!Objects.equals(match.getTournamentId(), tournament.getId())) {
            throw new NotFoundException("match not found");
        }
        if (match.getStatus() != BracketMatchStatus.READY) {
            throw new ConflictException("match is not ready for a winner");
        }
        if (!Objects.equals(winnerEntryId, match.getEntryIdA()) && !Objects.equals(winnerEntryId, match.getEntryIdB())) {
            throw new BadRequestException("winner must be one of the two entries");
        }

        TournamentFormat format = tournament.getFormat();
        Integer bracketSize = tournament.getBracketSize();
        if (bracketSize == null || bracketSize < 2) {
            throw new ConflictException("bracket is not initialized");
        }
        int wbRounds = BracketMath.roundsForBracketSize(bracketSize);

        Instant now = Instant.now();
        String loserId = Objects.equals(winnerEntryId, match.getEntryIdA()) ? match.getEntryIdB() : match.getEntryIdA();

        match.setBettingWindowClosesAt(null);
        match.setWinnerEntryId(winnerEntryId);
        match.setStatus(BracketMatchStatus.COMPLETE);
        match.setUpdatedAt(now);
        bracketMatchRepository.save(match);
        betService.resolveMatchBets(match, now);

        if (format == TournamentFormat.ROUND_ROBIN) {
            if (allRoundRobinMatchesComplete(tournamentId)) {
                markTournamentCompleted(tournamentId, now);
            }
            return toMatchResponse(bracketMatchRepository.findById(matchId).orElseThrow());
        }

        if (format == TournamentFormat.DOUBLE_ELIM) {
            int m = bracketSize;
            BracketPool pool = match.getBracketPool();
            if (pool == BracketPool.GF) {
                markTournamentCompleted(tournamentId, now);
                return toMatchResponse(bracketMatchRepository.findById(matchId).orElseThrow());
            }
            if (pool == BracketPool.LB) {
                onDoubleElimLbMatchComplete(tournamentId, m, match, winnerEntryId, now);
                return toMatchResponse(bracketMatchRepository.findById(matchId).orElseThrow());
            }
            // WB
            if (match.getRound() == wbRounds) {
                onDoubleElimWinnersFinalComplete(tournamentId, m, match, winnerEntryId, loserId, now);
            } else {
                maybeAdvanceWinnersParent(tournamentId, match, wbRounds, TournamentFormat.DOUBLE_ELIM);
            }
            routeDoubleElimLoserFromWinnersMatch(tournamentId, m, match);
            seedDoubleElimGrandFinalIfReady(tournamentId, m);
            return toMatchResponse(bracketMatchRepository.findById(matchId).orElseThrow());
        }

        // SINGLE_ELIM (WB only)
        if (match.getRound() == wbRounds) {
            markTournamentCompleted(tournamentId, now);
        } else {
            maybeAdvanceWinnersParent(tournamentId, match, wbRounds, TournamentFormat.SINGLE_ELIM);
        }
        return toMatchResponse(bracketMatchRepository.findById(matchId).orElseThrow());
    }

    private boolean allRoundRobinMatchesComplete(String tournamentId) {
        List<BracketMatch> rr =
                bracketMatchRepository.findByTournamentIdOrderByBracketPoolAscRoundAscIndexInRoundAsc(tournamentId).stream()
                        .filter(m -> m.getBracketPool() == BracketPool.RR)
                        .toList();
        return !rr.isEmpty() && rr.stream().allMatch(m -> m.getStatus() == BracketMatchStatus.COMPLETE);
    }

    private void onDoubleElimWinnersFinalComplete(
            String tournamentId, int m, BracketMatch wbFinal, String winnerEntryId, String loserEntryId, Instant now) {
        placeEntry(tournamentId, BracketPool.GF, 1, 0, true, winnerEntryId, now);
        if (m == 2) {
            placeEntry(tournamentId, BracketPool.GF, 1, 0, false, loserEntryId, now);
        } else if (m == 4) {
            placeEntry(tournamentId, BracketPool.LB, 2, 0, false, loserEntryId, now);
        } else {
            placeEntry(tournamentId, BracketPool.LB, 4, 0, false, loserEntryId, now);
        }
        tryPromoteDoubleElimMatches(tournamentId, m, now);
    }

    private void routeDoubleElimLoserFromWinnersMatch(String tournamentId, int m, BracketMatch wb) {
        if (wb.getBracketPool() != BracketPool.WB || wb.getRound() == 0) {
            return;
        }
        Instant now = Instant.now();
        if (wb.getStatus() != BracketMatchStatus.COMPLETE || wb.getWinnerEntryId() == null) {
            return;
        }
        String loserId = Objects.equals(wb.getWinnerEntryId(), wb.getEntryIdA()) ? wb.getEntryIdB() : wb.getEntryIdA();
        if (loserId == null) {
            return;
        }

        if (m == 2) {
            return;
        }
        if (m == 4) {
            if (wb.getRound() == 1) {
                if (wb.getIndexInRound() == 0) {
                    placeEntry(tournamentId, BracketPool.LB, 1, 0, true, loserId, now);
                } else {
                    placeEntry(tournamentId, BracketPool.LB, 1, 0, false, loserId, now);
                }
            }
            return;
        }
        // m == 8
        if (wb.getRound() == 1) {
            switch (wb.getIndexInRound()) {
                case 0 -> placeEntry(tournamentId, BracketPool.LB, 1, 0, true, loserId, now);
                case 1 -> placeEntry(tournamentId, BracketPool.LB, 1, 0, false, loserId, now);
                case 2 -> placeEntry(tournamentId, BracketPool.LB, 1, 1, true, loserId, now);
                case 3 -> placeEntry(tournamentId, BracketPool.LB, 1, 1, false, loserId, now);
                default -> {}
            }
        } else if (wb.getRound() == 2) {
            if (wb.getIndexInRound() == 0) {
                placeEntry(tournamentId, BracketPool.LB, 2, 0, false, loserId, now);
            } else {
                placeEntry(tournamentId, BracketPool.LB, 2, 1, false, loserId, now);
            }
        }
    }

    private void onDoubleElimLbMatchComplete(String tournamentId, int m, BracketMatch lb, String winnerEntryId, Instant now) {
        int r = lb.getRound();
        int i = lb.getIndexInRound();
        if (m == 4) {
            if (r == 1 && i == 0) {
                placeEntry(tournamentId, BracketPool.LB, 2, 0, true, winnerEntryId, now);
            } else if (r == 2 && i == 0) {
                placeEntry(tournamentId, BracketPool.GF, 1, 0, false, winnerEntryId, now);
            }
        } else if (m == 8) {
            if (r == 1 && i == 0) {
                placeEntry(tournamentId, BracketPool.LB, 2, 0, true, winnerEntryId, now);
            } else if (r == 1 && i == 1) {
                placeEntry(tournamentId, BracketPool.LB, 2, 1, true, winnerEntryId, now);
            } else if (r == 2 && i == 0) {
                appendWinnerToLbMerge(tournamentId, 3, 0, winnerEntryId, now);
            } else if (r == 2 && i == 1) {
                appendWinnerToLbMerge(tournamentId, 3, 0, winnerEntryId, now);
            } else if (r == 3 && i == 0) {
                placeEntry(tournamentId, BracketPool.LB, 4, 0, true, winnerEntryId, now);
            } else if (r == 4 && i == 0) {
                placeEntry(tournamentId, BracketPool.GF, 1, 0, false, winnerEntryId, now);
            }
        }
        tryPromoteDoubleElimMatches(tournamentId, m, now);
    }

    /** Fills slot A then B of a LB merge match with two incoming winners. */
    private void appendWinnerToLbMerge(String tournamentId, int destRound, int destIndex, String winnerEntryId, Instant now) {
        BracketMatch dest = loadMatch(tournamentId, BracketPool.LB, destRound, destIndex).orElseThrow();
        if (dest.getEntryIdA() == null) {
            dest.setEntryIdA(winnerEntryId);
        } else if (dest.getEntryIdB() == null) {
            dest.setEntryIdB(winnerEntryId);
        }
        promoteToReady(dest, now);
        dest.setUpdatedAt(now);
        bracketMatchRepository.save(dest);
    }

    private void seedDoubleElimGrandFinalIfReady(String tournamentId, int m) {
        tryPromoteDoubleElimMatches(tournamentId, m, Instant.now());
    }

    private void tryPromoteDoubleElimMatches(String tournamentId, int m, Instant now) {
        if (m == 2) {
            loadMatch(tournamentId, BracketPool.GF, 1, 0)
                    .ifPresent(g -> persistAfterPromote(g, now));
            return;
        }
        for (BracketMatch lb : bracketMatchRepository.findByTournamentIdOrderByBracketPoolAscRoundAscIndexInRoundAsc(tournamentId)) {
            if (lb.getBracketPool() == BracketPool.LB) {
                persistAfterPromote(lb, now);
            }
        }
        loadMatch(tournamentId, BracketPool.GF, 1, 0).ifPresent(g -> persistAfterPromote(g, now));
    }

    private void persistAfterPromote(BracketMatch m, Instant now) {
        promoteToReady(m, now);
        m.setUpdatedAt(now);
        bracketMatchRepository.save(m);
    }

    private void placeEntry(String tournamentId, BracketPool pool, int round, int index, boolean slotA, String entryId, Instant now) {
        BracketMatch m = loadMatch(tournamentId, pool, round, index).orElseThrow(() -> new NotFoundException("bracket match not found"));
        if (slotA) {
            if (m.getEntryIdA() == null) {
                m.setEntryIdA(entryId);
            }
        } else {
            if (m.getEntryIdB() == null) {
                m.setEntryIdB(entryId);
            }
        }
        promoteToReady(m, now);
        m.setUpdatedAt(now);
        bracketMatchRepository.save(m);
    }

    private void promoteToReady(BracketMatch m, Instant now) {
        if (m.getStatus() == BracketMatchStatus.COMPLETE) {
            return;
        }
        if (m.getEntryIdA() == null && m.getEntryIdB() != null) {
            m.setWinnerEntryId(m.getEntryIdB());
            m.setStatus(BracketMatchStatus.COMPLETE);
            m.setUpdatedAt(now);
        } else if (m.getEntryIdB() == null && m.getEntryIdA() != null) {
            m.setWinnerEntryId(m.getEntryIdA());
            m.setStatus(BracketMatchStatus.COMPLETE);
            m.setUpdatedAt(now);
        } else if (m.getEntryIdA() != null && m.getEntryIdB() != null) {
            m.setStatus(BracketMatchStatus.READY);
            stampBettingSchedule(m, now);
        } else {
            m.setStatus(BracketMatchStatus.WAITING);
        }
    }

    private void stampBettingSchedule(BracketMatch m, Instant now) {
        if (m.getScheduledStartAt() != null || m.getStatus() != BracketMatchStatus.READY) {
            return;
        }
        Tournament t = tournamentRepository.findById(m.getTournamentId()).orElseThrow();
        long idx = MatchBettingSchedule.slotIndexFor(m.getBracketPool(), m.getRound(), m.getIndexInRound());
        m.setScheduledStartAt(MatchBettingSchedule.scheduledStartForSlot(t, idx, bettingConfig.slotStaggerMinutes(), now));
    }

    private void maybeAdvanceWinnersParent(String tournamentId, BracketMatch child, int totalWbRounds, TournamentFormat format) {
        if (child.getRound() >= totalWbRounds) {
            return;
        }
        int r = child.getRound();
        int c = child.getIndexInRound();
        int pr = r + 1;
        int pi = c >> 1;

        BracketMatch left = loadMatch(tournamentId, BracketPool.WB, r, 2 * pi).orElseThrow(() -> new NotFoundException("bracket match not found"));
        BracketMatch right = loadMatch(tournamentId, BracketPool.WB, r, 2 * pi + 1)
                .orElseThrow(() -> new NotFoundException("bracket match not found"));

        if (left.getStatus() != BracketMatchStatus.COMPLETE || right.getStatus() != BracketMatchStatus.COMPLETE) {
            return;
        }
        if (left.getWinnerEntryId() == null || right.getWinnerEntryId() == null) {
            return;
        }

        BracketMatch parent = loadMatch(tournamentId, BracketPool.WB, pr, pi).orElseThrow(() -> new NotFoundException("bracket match not found"));

        Instant now = Instant.now();
        parent.setEntryIdA(left.getWinnerEntryId());
        parent.setEntryIdB(right.getWinnerEntryId());

        if (parent.getEntryIdA() == null && parent.getEntryIdB() != null) {
            parent.setWinnerEntryId(parent.getEntryIdB());
            parent.setStatus(BracketMatchStatus.COMPLETE);
        } else if (parent.getEntryIdB() == null && parent.getEntryIdA() != null) {
            parent.setWinnerEntryId(parent.getEntryIdA());
            parent.setStatus(BracketMatchStatus.COMPLETE);
        } else if (parent.getEntryIdA() != null && parent.getEntryIdB() != null) {
            parent.setStatus(BracketMatchStatus.READY);
            stampBettingSchedule(parent, now);
        } else {
            parent.setStatus(BracketMatchStatus.WAITING);
        }
        parent.setUpdatedAt(now);
        bracketMatchRepository.save(parent);

        if (parent.getStatus() == BracketMatchStatus.COMPLETE) {
            if (format == TournamentFormat.SINGLE_ELIM && parent.getRound() == totalWbRounds) {
                markTournamentCompleted(tournamentId, now);
                return;
            }
            maybeAdvanceWinnersParent(tournamentId, parent, totalWbRounds, format);
            if (format == TournamentFormat.DOUBLE_ELIM) {
                routeDoubleElimLoserFromWinnersMatch(tournamentId, tournamentRepository.findById(tournamentId).orElseThrow().getBracketSize(), parent);
                seedDoubleElimGrandFinalIfReady(tournamentId, tournamentRepository.findById(tournamentId).orElseThrow().getBracketSize());
            }
        }
    }

    private Optional<BracketMatch> loadMatch(String tournamentId, BracketPool pool, int round, int index) {
        Optional<BracketMatch> o = bracketMatchRepository.findByTournamentIdAndBracketPoolAndRoundAndIndexInRound(tournamentId, pool, round, index);
        if (o.isEmpty() && pool == BracketPool.WB) {
            return bracketMatchRepository.findByTournamentIdAndRoundAndIndexInRound(tournamentId, round, index);
        }
        return o;
    }

    private void applyByeOrReady(BracketMatch bm, Instant now) {
        String a = bm.getEntryIdA();
        String b = bm.getEntryIdB();
        if (a == null && b == null) {
            throw new BadRequestException("invalid bracket seed");
        }
        if (a == null) {
            bm.setWinnerEntryId(b);
            bm.setStatus(BracketMatchStatus.COMPLETE);
        } else if (b == null) {
            bm.setWinnerEntryId(a);
            bm.setStatus(BracketMatchStatus.COMPLETE);
        } else {
            bm.setStatus(BracketMatchStatus.READY);
            stampBettingSchedule(bm, now);
        }
    }

    private void assertDbAdmin(JwtPrincipal principal) {
        User u = userRepository.findById(principal.userId()).orElseThrow(() -> new NotFoundException("user not found"));
        UserRole role = u.getRole() == null ? UserRole.PLAYER : u.getRole();
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException("admin only");
        }
    }

    private BracketMatchResponse toMatchResponse(BracketMatch m) {
        BetService.MatchStakeBoard board = betService.stakeBoardForMatch(m);
        return new BracketMatchResponse(
                m.getId(),
                m.getTournamentId(),
                m.getBracketPool(),
                m.getRound(),
                m.getIndexInRound(),
                m.getEntryIdA(),
                m.getEntryIdB(),
                m.getWinnerEntryId(),
                m.getStatus(),
                m.getScheduledStartAt(),
                board.bettingWindowMinutes(),
                board.bettingClosesAt(),
                board.stakeOnA(),
                board.stakeOnB(),
                board.impliedReturnPerCoinOnA(),
                board.impliedReturnPerCoinOnB()
        );
    }

    private static TournamentResponse toTournamentResponse(Tournament t) {
        return TournamentResponses.from(t);
    }

    private void markTournamentCompleted(String tournamentId, Instant now) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        if (t.getLifecycleStatus() != TournamentLifecycleStatus.COMPLETED) {
            t.setLifecycleStatus(TournamentLifecycleStatus.COMPLETED);
            t.setUpdatedAt(now);
            tournamentRepository.save(t);
            trophyAwardIssuanceService.issueIfNeeded(tournamentId);
        }
    }
}
