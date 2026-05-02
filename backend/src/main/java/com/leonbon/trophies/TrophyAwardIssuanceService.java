package com.leonbon.trophies;

import com.leonbon.tournaments.BracketMatch;
import com.leonbon.tournaments.BracketMatchRepository;
import com.leonbon.tournaments.BracketMatchStatus;
import com.leonbon.tournaments.BracketMath;
import com.leonbon.tournaments.BracketPool;
import com.leonbon.tournaments.Tournament;
import com.leonbon.tournaments.TournamentEntry;
import com.leonbon.tournaments.TournamentEntryRepository;
import com.leonbon.tournaments.TournamentEntryStatus;
import com.leonbon.tournaments.TournamentEntryType;
import com.leonbon.tournaments.TournamentFormat;
import com.leonbon.tournaments.TournamentLifecycleStatus;
import com.leonbon.tournaments.TournamentPlacementPrizeService;
import com.leonbon.tournaments.TournamentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TrophyAwardIssuanceService {
    private final TrophyAwardRepository trophyAwardRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final BracketMatchRepository bracketMatchRepository;
    private final TournamentPlacementPrizeService tournamentPlacementPrizeService;

    public TrophyAwardIssuanceService(
            TrophyAwardRepository trophyAwardRepository,
            TournamentRepository tournamentRepository,
            TournamentEntryRepository tournamentEntryRepository,
            BracketMatchRepository bracketMatchRepository,
            TournamentPlacementPrizeService tournamentPlacementPrizeService
    ) {
        this.trophyAwardRepository = trophyAwardRepository;
        this.tournamentRepository = tournamentRepository;
        this.tournamentEntryRepository = tournamentEntryRepository;
        this.bracketMatchRepository = bracketMatchRepository;
        this.tournamentPlacementPrizeService = tournamentPlacementPrizeService;
    }

    /**
     * Invocar una sola vez cuando el torneo acaba de pasar a COMPLETED. Idempotente por {@code tournamentId}.
     */
    public void issueIfNeeded(String tournamentId) {
        if (!trophyAwardRepository.existsByTournamentId(tournamentId)) {
            Tournament t = tournamentRepository.findById(tournamentId).orElse(null);
            if (t == null || t.getLifecycleStatus() != TournamentLifecycleStatus.COMPLETED) {
                return;
            }

            List<BracketMatch> matches =
                    bracketMatchRepository.findByTournamentIdOrderByBracketPoolAscRoundAscIndexInRoundAsc(tournamentId);
            Instant now = Instant.now();
            List<TrophyAward> out = new ArrayList<>();
            switch (t.getFormat()) {
                case SINGLE_ELIM -> emitSingleElim(t, matches, out, now);
                case DOUBLE_ELIM -> emitDoubleElim(t, matches, out, now);
                case ROUND_ROBIN -> emitRoundRobin(t, matches, out, now);
            }
            if (!out.isEmpty()) {
                trophyAwardRepository.saveAll(out);
            }
        }
        tournamentPlacementPrizeService.payOutIfNeeded(tournamentId);
    }

    private void emitSingleElim(Tournament t, List<BracketMatch> matches, List<TrophyAward> out, Instant now) {
        Integer m = t.getBracketSize();
        if (m == null || m < 2) {
            return;
        }
        int wbRounds = BracketMath.roundsForBracketSize(m);
        Optional<BracketMatch> fin = matches.stream()
                .filter(x -> x.getStatus() == BracketMatchStatus.COMPLETE)
                .filter(x -> poolOrWb(x.getBracketPool()))
                .filter(x -> x.getRound() == wbRounds)
                .findFirst();
        if (fin.isEmpty()) {
            return;
        }
        BracketMatch f = fin.orElseThrow();
        addTopTwoFromMatch(t, f, out, now);
    }

    private void emitDoubleElim(Tournament t, List<BracketMatch> matches, List<TrophyAward> out, Instant now) {
        Optional<BracketMatch> gf =
                matches.stream()
                        .filter(x -> x.getBracketPool() == BracketPool.GF)
                        .filter(x -> x.getStatus() == BracketMatchStatus.COMPLETE)
                        .findFirst();
        if (gf.isEmpty()) {
            return;
        }
        addTopTwoFromMatch(t, gf.orElseThrow(), out, now);
    }

    private void emitRoundRobin(Tournament t, List<BracketMatch> matches, List<TrophyAward> out, Instant now) {
        List<BracketMatch> rr = matches.stream()
                .filter(x -> x.getBracketPool() == BracketPool.RR)
                .filter(x -> x.getStatus() == BracketMatchStatus.COMPLETE)
                .toList();
        if (rr.isEmpty()) {
            return;
        }
        Map<String, Long> wins = new HashMap<>();
        for (BracketMatch m : rr) {
            String w = m.getWinnerEntryId();
            if (w == null || w.isBlank()) {
                continue;
            }
            wins.merge(w, 1L, Long::sum);
        }
        List<TournamentEntry> approved =
                tournamentEntryRepository.findByTournamentIdOrderByCreatedAtAsc(t.getId()).stream()
                        .filter(e -> e.getStatus() == TournamentEntryStatus.APPROVED)
                        .toList();
        for (TournamentEntry e : approved) {
            wins.putIfAbsent(e.getId(), 0L);
        }
        if (wins.isEmpty()) {
            return;
        }
        List<Map.Entry<String, Long>> sorted =
                wins.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).toList();
        String firstId = sorted.get(0).getKey();
        maybeAddPlacement(t, tournamentEntryRepository.findById(firstId).orElse(null), 1, "Campeón (liga)", out, now);

        if (sorted.size() >= 2) {
            long top = sorted.get(0).getValue();
            String secondId = sorted.stream()
                    .filter(e -> e.getValue() < top)
                    .findFirst()
                    .map(Map.Entry::getKey)
                    .orElse(sorted.get(1).getKey());
            if (!Objects.equals(secondId, firstId)) {
                maybeAddPlacement(
                        t,
                        tournamentEntryRepository.findById(secondId).orElse(null),
                        2,
                        "Segundo lugar (liga)",
                        out,
                        now);
            }
        }
    }

    private void addTopTwoFromMatch(Tournament t, BracketMatch f, List<TrophyAward> out, Instant now) {
        String wId = f.getWinnerEntryId();
        if (wId == null || wId.isBlank()) {
            return;
        }
        TournamentEntry winE = tournamentEntryRepository.findById(wId).orElse(null);
        maybeAddPlacement(t, winE, 1, "Campeón", out, now);

        String a = f.getEntryIdA();
        String b = f.getEntryIdB();
        if (a != null && b != null && !a.isBlank() && !b.isBlank()) {
            String loseId = Objects.equals(wId, a) ? b : a;
            TournamentEntry loseE = tournamentEntryRepository.findById(loseId).orElse(null);
            maybeAddPlacement(t, loseE, 2, "Subcampeón", out, now);
        }
    }

    private void maybeAddPlacement(
            Tournament t, TournamentEntry entry, int placement, String label, List<TrophyAward> out, Instant now) {
        if (entry == null) {
            return;
        }
        TrophyAward row = new TrophyAward();
        row.setTournamentId(t.getId());
        row.setTournamentName(t.getName());
        row.setGame(t.getGame());
        row.setTournamentFormat(t.getFormat());
        row.setPlacement(placement);
        row.setBadgeLabel(label);
        row.setTournamentEntryId(entry.getId());
        row.setEntryType(entry.getType());
        row.setAwardedAt(now);
        if (entry.getType() == TournamentEntryType.TEAM) {
            row.setTeamId(entry.getTeamId());
            row.setPlayerId(null);
            List<String> roster = entry.getSelectedRosterUserIds();
            row.setCreditedMemberUserIds(
                    roster == null ? List.of() : roster.stream().filter(Objects::nonNull).distinct().toList());
        } else {
            row.setTeamId(null);
            row.setPlayerId(entry.getPlayerId());
            String pid = entry.getPlayerId();
            row.setCreditedMemberUserIds(
                    pid == null || pid.isBlank() ? List.of() : new ArrayList<>(List.of(pid)));
        }
        out.add(row);
    }

    private static boolean poolOrWb(BracketPool p) {
        return p == null || p == BracketPool.WB;
    }

    public List<TrophyAward> listMergedForMemberUser(String userId) {
        List<TrophyAward> asPlayer = trophyAwardRepository.findByPlayerIdOrderByAwardedAtDesc(userId);
        List<TrophyAward> rostered = trophyAwardRepository.findByCreditedMemberUserIdsContainingOrderByAwardedAtDesc(userId);

        Map<String, TrophyAward> uniq = new java.util.LinkedHashMap<>();
        for (TrophyAward a : asPlayer) {
            uniq.putIfAbsent(a.getId(), a);
        }
        for (TrophyAward a : rostered) {
            uniq.putIfAbsent(a.getId(), a);
        }
        return uniq.values().stream().sorted(Comparator.comparing(TrophyAward::getAwardedAt).reversed()).toList();
    }

    public List<TrophyAward> listForTeamMembersView(String teamId) {
        return trophyAwardRepository.findByTeamIdOrderByAwardedAtDesc(teamId).stream().toList();
    }

    public static List<com.leonbon.trophies.dto.TrophyAwardResponse> mapResponses(List<TrophyAward> rows) {
        return rows.stream()
                .map(a ->
                        new com.leonbon.trophies.dto.TrophyAwardResponse(
                                a.getId(),
                                a.getTournamentId(),
                                a.getTournamentName(),
                                a.getGame(),
                                a.getTournamentFormat(),
                                a.getPlacement(),
                                a.getBadgeLabel(),
                                a.getTournamentEntryId(),
                                a.getEntryType(),
                                a.getTeamId(),
                                a.getPlayerId(),
                                a.getAwardedAt()))
                .toList();
    }
}
