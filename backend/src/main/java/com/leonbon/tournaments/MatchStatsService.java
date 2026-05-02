package com.leonbon.tournaments;

import com.leonbon.auth.ConflictException;
import com.leonbon.auth.JwtPrincipal;
import com.leonbon.tournaments.dto.BracketMatchStatsResponse;
import com.leonbon.tournaments.dto.UpsertBracketMatchStatsRequest;
import com.leonbon.tournaments.stats.BracketMatchStats;
import com.leonbon.tournaments.stats.BracketMatchStatsRepository;
import com.leonbon.tournaments.stats.MatchStatsFortniteRow;
import com.leonbon.tournaments.stats.MatchStatsMlbRow;
import com.leonbon.tournaments.stats.MatchStatsValorantRow;
import com.leonbon.users.User;
import com.leonbon.users.UserRepository;
import com.leonbon.users.UserRole;
import com.leonbon.web.BadRequestException;
import com.leonbon.web.ForbiddenException;
import com.leonbon.web.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MatchStatsService {
    private final UserRepository userRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final BracketMatchRepository bracketMatchRepository;
    private final BracketMatchStatsRepository bracketMatchStatsRepository;

    public MatchStatsService(
            UserRepository userRepository,
            TournamentRepository tournamentRepository,
            TournamentEntryRepository tournamentEntryRepository,
            BracketMatchRepository bracketMatchRepository,
            BracketMatchStatsRepository bracketMatchStatsRepository
    ) {
        this.userRepository = userRepository;
        this.tournamentRepository = tournamentRepository;
        this.tournamentEntryRepository = tournamentEntryRepository;
        this.bracketMatchRepository = bracketMatchRepository;
        this.bracketMatchStatsRepository = bracketMatchStatsRepository;
    }

    public BracketMatchStatsResponse getPublic(String tournamentId, String matchId) {
        tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        bracketMatchRepository
                .findById(matchId)
                .filter(m -> Objects.equals(m.getTournamentId(), tournamentId))
                .orElseThrow(() -> new NotFoundException("match not found"));
        BracketMatchStats stats = bracketMatchStatsRepository
                .findByMatchId(matchId)
                .filter(s -> Objects.equals(s.getTournamentId(), tournamentId))
                .orElseThrow(() -> new NotFoundException("match stats not found"));
        return toResponse(stats);
    }

    public Optional<BracketMatchStatsResponse> upsertAsAdmin(
            JwtPrincipal admin, String tournamentId, String matchId, UpsertBracketMatchStatsRequest req) {
        assertDbAdmin(admin);
        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        BracketMatch match = bracketMatchRepository.findById(matchId).orElseThrow(() -> new NotFoundException("match not found"));
        if (!Objects.equals(match.getTournamentId(), tournamentId)) {
            throw new NotFoundException("match not found");
        }
        if (match.getStatus() != BracketMatchStatus.COMPLETE) {
            throw new ConflictException("stats can only be saved for COMPLETE matches");
        }
        if (match.getWinnerEntryId() == null || match.getEntryIdA() == null || match.getEntryIdB() == null) {
            throw new BadRequestException("match must have resolved entries before stats");
        }

        int payloads =
                (req.getValorantPlayers() != null ? 1 : 0)
                        + (req.getFortnitePlayers() != null ? 1 : 0)
                        + (req.getMlbPlayers() != null ? 1 : 0);
        if (payloads != 1) {
            throw new BadRequestException("send exactly one of valorantPlayers, fortnitePlayers, mlbPlayers");
        }

        GameTitle tg = tournament.getGame();
        if (tg == GameTitle.VALORANT && req.getValorantPlayers() == null) {
            throw new BadRequestException("valorantPlayers required for VALORANT tournament");
        }
        if (tg == GameTitle.FORTNITE && req.getFortnitePlayers() == null) {
            throw new BadRequestException("fortnitePlayers required for FORTNITE tournament");
        }
        if (tg == GameTitle.MLB && req.getMlbPlayers() == null) {
            throw new BadRequestException("mlbPlayers required for MLB tournament");
        }

        Set<String> allowed = eligibleParticipantUserIds(match, tournament);
        if (allowed.isEmpty()) {
            throw new BadRequestException("could not derive roster/player ids from match entries");
        }

        Instant now = Instant.now();

        if (tg == GameTitle.VALORANT) {
            List<UpsertBracketMatchStatsRequest.ValorantPlayerIn> rows = Objects.requireNonNull(req.getValorantPlayers());
            if (rows.isEmpty()) {
                bracketMatchStatsRepository.deleteByMatchId(matchId);
                return Optional.empty();
            }
            assertDistinctUserIdsMapped(rows.stream().map(UpsertBracketMatchStatsRequest.ValorantPlayerIn::getUserId).toList());
            for (UpsertBracketMatchStatsRequest.ValorantPlayerIn r : rows) {
                assertEligibleUser(r.getUserId(), allowed);
            }
            List<MatchStatsValorantRow> out = new ArrayList<>();
            for (UpsertBracketMatchStatsRequest.ValorantPlayerIn r : rows) {
                MatchStatsValorantRow row = new MatchStatsValorantRow();
                row.setUserId(r.getUserId().trim());
                row.setKda(r.getKda());
                row.setKills(r.getKills());
                row.setDeaths(r.getDeaths());
                row.setAssists(r.getAssists());
                row.setHeadshotPct(r.getHeadshotPct());
                out.add(row);
            }
            saveBundle(admin.userId(), now, tournamentId, matchId, tg, null, null, out);
        } else if (tg == GameTitle.FORTNITE) {
            List<UpsertBracketMatchStatsRequest.FortnitePlayerIn> rows = Objects.requireNonNull(req.getFortnitePlayers());
            if (rows.isEmpty()) {
                bracketMatchStatsRepository.deleteByMatchId(matchId);
                return Optional.empty();
            }
            assertDistinctUserIdsMapped(rows.stream().map(UpsertBracketMatchStatsRequest.FortnitePlayerIn::getUserId).toList());
            for (UpsertBracketMatchStatsRequest.FortnitePlayerIn r : rows) {
                assertEligibleUser(r.getUserId(), allowed);
            }
            List<MatchStatsFortniteRow> out = new ArrayList<>();
            for (UpsertBracketMatchStatsRequest.FortnitePlayerIn r : rows) {
                MatchStatsFortniteRow row = new MatchStatsFortniteRow();
                row.setUserId(r.getUserId().trim());
                row.setKills(r.getKills());
                row.setDeaths(r.getDeaths());
                row.setPlacement(r.getPlacement());
                row.setModePlayed(trimNullable(r.getModePlayed()));
                out.add(row);
            }
            saveBundle(admin.userId(), now, tournamentId, matchId, tg, null, out, null);
        } else {
            List<UpsertBracketMatchStatsRequest.MlbPlayerIn> rows = Objects.requireNonNull(req.getMlbPlayers());
            if (rows.isEmpty()) {
                bracketMatchStatsRepository.deleteByMatchId(matchId);
                return Optional.empty();
            }
            assertDistinctUserIdsMapped(rows.stream().map(UpsertBracketMatchStatsRequest.MlbPlayerIn::getUserId).toList());
            for (UpsertBracketMatchStatsRequest.MlbPlayerIn r : rows) {
                assertEligibleUser(r.getUserId(), allowed);
            }
            List<MatchStatsMlbRow> out = new ArrayList<>();
            for (UpsertBracketMatchStatsRequest.MlbPlayerIn r : rows) {
                MatchStatsMlbRow row = new MatchStatsMlbRow();
                row.setUserId(r.getUserId().trim());
                row.setBattingAvgGame(r.getBattingAvgGame());
                row.setHomeRunsGame(r.getHomeRunsGame());
                row.setInningsPitchedGame(r.getInningsPitchedGame());
                row.setEraGame(r.getEraGame());
                row.setRunsAllowedGame(r.getRunsAllowedGame());
                out.add(row);
            }
            saveBundle(admin.userId(), now, tournamentId, matchId, tg, out, null, null);
        }

        BracketMatchStats persisted = bracketMatchStatsRepository.findByMatchId(matchId).orElseThrow();
        return Optional.of(toResponse(persisted));
    }

    private void saveBundle(
            String adminUserId,
            Instant now,
            String tournamentId,
            String matchId,
            GameTitle game,
            List<MatchStatsMlbRow> mlb,
            List<MatchStatsFortniteRow> fn,
            List<MatchStatsValorantRow> val
    ) {
        BracketMatchStats stats = bracketMatchStatsRepository.findByMatchId(matchId).orElse(null);
        if (stats == null) {
            stats = new BracketMatchStats();
            stats.setMatchId(matchId);
            stats.setRevision(0);
        }
        stats.setTournamentId(tournamentId);
        stats.setGame(game);
        stats.setRecordedByAdminUserId(adminUserId);
        stats.setRevision(stats.getRevision() + 1);
        stats.setRecordedAt(now);

        stats.setValorantRows(val == null ? List.of() : val);
        stats.setFortniteRows(fn == null ? List.of() : fn);
        stats.setMlbRows(mlb == null ? List.of() : mlb);

        bracketMatchStatsRepository.save(stats);
    }

    private static void assertEligibleUser(String userId, Set<String> allowed) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required");
        }
        String t = userId.trim();
        if (!allowed.contains(t)) {
            throw new BadRequestException("userId not part of match roster: " + t);
        }
    }

    private static void assertDistinctUserIdsMapped(List<String> raw) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String u : raw) {
            String t = u == null ? "" : u.trim();
            if (t.isEmpty()) throw new BadRequestException("userId must be non-empty in each row");
            if (!seen.add(t)) {
                throw new BadRequestException("duplicate userId in payload: " + t);
            }
        }
    }

    private Set<String> eligibleParticipantUserIds(BracketMatch match, Tournament tournament) {
        Set<String> out = new LinkedHashSet<>();
        TournamentEntry ea = tournamentEntryRepository.findById(match.getEntryIdA()).orElseThrow(() -> new NotFoundException("entry A not found"));
        TournamentEntry eb = tournamentEntryRepository.findById(match.getEntryIdB()).orElseThrow(() -> new NotFoundException("entry B not found"));

        if (tournament.getGame() == GameTitle.MLB) {
            if (ea.getPlayerId() != null) out.add(ea.getPlayerId());
            if (eb.getPlayerId() != null) out.add(eb.getPlayerId());
        } else {
            if (ea.getSelectedRosterUserIds() != null) {
                for (String u : ea.getSelectedRosterUserIds()) {
                    if (u != null && !u.isBlank()) {
                        out.add(u.trim());
                    }
                }
            }
            if (eb.getSelectedRosterUserIds() != null) {
                for (String u : eb.getSelectedRosterUserIds()) {
                    if (u != null && !u.isBlank()) {
                        out.add(u.trim());
                    }
                }
            }
        }
        out.removeIf(Objects::isNull);
        return out;
    }

    private BracketMatchStatsResponse toResponse(BracketMatchStats s) {
        List<BracketMatchStatsResponse.ValorantPlayerStatsOut> v = s.getValorantRows() == null
                ? List.of()
                : s.getValorantRows().stream()
                        .map(r -> new BracketMatchStatsResponse.ValorantPlayerStatsOut(
                                r.getUserId(), r.getKda(), r.getKills(), r.getDeaths(), r.getAssists(), r.getHeadshotPct()))
                        .toList();
        List<BracketMatchStatsResponse.FortnitePlayerStatsOut> f = s.getFortniteRows() == null
                ? List.of()
                : s.getFortniteRows().stream()
                        .map(r -> new BracketMatchStatsResponse.FortnitePlayerStatsOut(
                                r.getUserId(),
                                r.getKills(),
                                r.getDeaths(),
                                r.getPlacement(),
                                r.getModePlayed()))
                        .toList();
        List<BracketMatchStatsResponse.MlbPlayerStatsOut> m = s.getMlbRows() == null
                ? List.of()
                : s.getMlbRows().stream()
                        .map(r -> new BracketMatchStatsResponse.MlbPlayerStatsOut(
                                r.getUserId(),
                                r.getBattingAvgGame(),
                                r.getHomeRunsGame(),
                                r.getInningsPitchedGame(),
                                r.getEraGame(),
                                r.getRunsAllowedGame()))
                        .toList();

        return new BracketMatchStatsResponse(
                s.getMatchId(),
                s.getTournamentId(),
                s.getGame(),
                s.getRevision(),
                s.getRecordedByAdminUserId(),
                s.getRecordedAt(),
                v,
                f,
                m);
    }

    private static String trimNullable(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void assertDbAdmin(JwtPrincipal principal) {
        User u = userRepository.findById(principal.userId()).orElseThrow(() -> new NotFoundException("user not found"));
        UserRole role = u.getRole() == null ? UserRole.PLAYER : u.getRole();
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException("admin only");
        }
    }
}
