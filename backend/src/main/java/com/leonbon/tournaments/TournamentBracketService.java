package com.leonbon.tournaments;

import com.leonbon.auth.ConflictException;
import com.leonbon.auth.JwtPrincipal;
import com.leonbon.tournaments.dto.BracketMatchResponse;
import com.leonbon.tournaments.dto.TournamentResponse;
import com.leonbon.users.User;
import com.leonbon.users.UserRepository;
import com.leonbon.users.UserRole;
import com.leonbon.web.BadRequestException;
import com.leonbon.web.ForbiddenException;
import com.leonbon.web.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class TournamentBracketService {
    private final UserRepository userRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final BracketMatchRepository bracketMatchRepository;

    public TournamentBracketService(
            UserRepository userRepository,
            TournamentRepository tournamentRepository,
            TournamentEntryRepository tournamentEntryRepository,
            BracketMatchRepository bracketMatchRepository
    ) {
        this.userRepository = userRepository;
        this.tournamentRepository = tournamentRepository;
        this.tournamentEntryRepository = tournamentEntryRepository;
        this.bracketMatchRepository = bracketMatchRepository;
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

    public TournamentResponse generateSingleElimBracketAsAdmin(JwtPrincipal admin, String tournamentId) {
        assertDbAdmin(admin);
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        if (t.getFormat() != TournamentFormat.SINGLE_ELIM) {
            throw new BadRequestException("MVP4 bracket generation supports SINGLE_ELIM only");
        }
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

        int n = approved.size();
        int m = nextPow2(n);
        int totalRounds = roundsForBracketSize(m);

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
            bm.setRound(1);
            bm.setIndexInRound(i);
            String a = padded.get(2 * i);
            String b = padded.get(2 * i + 1);
            bm.setEntryIdA(a);
            bm.setEntryIdB(b);
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
            }
            bm.setCreatedAt(now);
            bm.setUpdatedAt(now);
            bracketMatchRepository.save(bm);
        }

        for (int r = 2; r <= totalRounds; r++) {
            int count = m >> r;
            for (int i = 0; i < count; i++) {
                BracketMatch bm = new BracketMatch();
                bm.setTournamentId(t.getId());
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
            BracketMatch child = bracketMatchRepository
                    .findByTournamentIdAndRoundAndIndexInRound(t.getId(), 1, i)
                    .orElseThrow();
            if (child.getStatus() == BracketMatchStatus.COMPLETE) {
                maybeAdvanceParent(t.getId(), child, totalRounds);
            }
        }

        t = tournamentRepository.findById(tournamentId).orElseThrow();
        t.setLifecycleStatus(TournamentLifecycleStatus.LIVE);
        t.setUpdatedAt(Instant.now());
        tournamentRepository.save(t);

        return toTournamentResponse(tournamentRepository.findById(tournamentId).orElseThrow());
    }

    public List<BracketMatchResponse> listBracketMatches(String tournamentId) {
        tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        return bracketMatchRepository.findByTournamentIdOrderByRoundAscIndexInRoundAsc(tournamentId).stream()
                .map(this::toMatchResponse)
                .toList();
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

        Integer bracketSize = tournament.getBracketSize();
        if (bracketSize == null || bracketSize < 2) {
            throw new ConflictException("bracket is not initialized");
        }
        int totalRounds = roundsForBracketSize(bracketSize);

        Instant now = Instant.now();
        match.setWinnerEntryId(winnerEntryId);
        match.setStatus(BracketMatchStatus.COMPLETE);
        match.setUpdatedAt(now);
        bracketMatchRepository.save(match);

        if (match.getRound() == totalRounds) {
            markTournamentCompleted(tournamentId, now);
        } else {
            maybeAdvanceParent(tournamentId, match, totalRounds);
        }

        return toMatchResponse(bracketMatchRepository.findById(matchId).orElseThrow());
    }

    private void maybeAdvanceParent(String tournamentId, BracketMatch child, int totalRounds) {
        if (child.getRound() == totalRounds && child.getStatus() == BracketMatchStatus.COMPLETE) {
            markTournamentCompleted(tournamentId, Instant.now());
            return;
        }
        if (child.getRound() >= totalRounds) {
            return;
        }
        int r = child.getRound();
        int c = child.getIndexInRound();
        int pr = r + 1;
        int pi = c >> 1;

        BracketMatch left = bracketMatchRepository
                .findByTournamentIdAndRoundAndIndexInRound(tournamentId, r, 2 * pi)
                .orElseThrow(() -> new NotFoundException("bracket match not found"));
        BracketMatch right = bracketMatchRepository
                .findByTournamentIdAndRoundAndIndexInRound(tournamentId, r, 2 * pi + 1)
                .orElseThrow(() -> new NotFoundException("bracket match not found"));

        if (left.getStatus() != BracketMatchStatus.COMPLETE || right.getStatus() != BracketMatchStatus.COMPLETE) {
            return;
        }
        if (left.getWinnerEntryId() == null || right.getWinnerEntryId() == null) {
            return;
        }

        BracketMatch parent = bracketMatchRepository
                .findByTournamentIdAndRoundAndIndexInRound(tournamentId, pr, pi)
                .orElseThrow(() -> new NotFoundException("bracket match not found"));

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
        } else {
            parent.setStatus(BracketMatchStatus.WAITING);
        }
        parent.setUpdatedAt(now);
        bracketMatchRepository.save(parent);

        if (parent.getStatus() == BracketMatchStatus.COMPLETE) {
            if (parent.getRound() == totalRounds) {
                markTournamentCompleted(tournamentId, now);
                return;
            }
            maybeAdvanceParent(tournamentId, parent, totalRounds);
        }
    }

    private void markTournamentCompleted(String tournamentId, Instant now) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow();
        if (t.getLifecycleStatus() != TournamentLifecycleStatus.COMPLETED) {
            t.setLifecycleStatus(TournamentLifecycleStatus.COMPLETED);
            t.setUpdatedAt(now);
            tournamentRepository.save(t);
        }
    }

    private static int nextPow2(int n) {
        if (n <= 1) {
            return 2;
        }
        int p = 1;
        while (p < n) {
            p <<= 1;
        }
        return p;
    }

    /** Number of rounds for a single-elim bracket of size {@code m} (power of two, m >= 2). */
    private static int roundsForBracketSize(int m) {
        int r = 0;
        for (int x = m; x > 1; x >>= 1) {
            r++;
        }
        return r;
    }

    private void assertDbAdmin(JwtPrincipal principal) {
        User u = userRepository.findById(principal.userId()).orElseThrow(() -> new NotFoundException("user not found"));
        UserRole role = u.getRole() == null ? UserRole.PLAYER : u.getRole();
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException("admin only");
        }
    }

    private BracketMatchResponse toMatchResponse(BracketMatch m) {
        return new BracketMatchResponse(
                m.getId(),
                m.getTournamentId(),
                m.getRound(),
                m.getIndexInRound(),
                m.getEntryIdA(),
                m.getEntryIdB(),
                m.getWinnerEntryId(),
                m.getStatus()
        );
    }

    private static TournamentResponse toTournamentResponse(Tournament t) {
        return new TournamentResponse(
                t.getId(),
                t.getName(),
                t.getOrganizers(),
                t.getGame(),
                t.getFormat(),
                t.getLifecycleStatus(),
                t.getRegistrationStartAt(),
                t.getRegistrationEndAt(),
                t.getCompetitionStartAt(),
                t.getCompetitionEndAt(),
                t.getStreamUrl(),
                t.getBracketSize(),
                t.getCreatedAt()
        );
    }
}
