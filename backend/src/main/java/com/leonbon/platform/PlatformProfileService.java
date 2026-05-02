package com.leonbon.platform;

import com.leonbon.platform.dto.GamePlatformRollupResponse;
import com.leonbon.platform.dto.InternalLeaderboardRowResponse;
import com.leonbon.platform.dto.PublicPlayerSheetResponse;
import com.leonbon.platform.dto.TeamAffiliationPublicResponse;
import com.leonbon.platform.dto.UserPlatformSnapshotResponse;
import com.leonbon.tournaments.BracketMatch;
import com.leonbon.tournaments.BracketMatchRepository;
import com.leonbon.tournaments.BracketMatchStatus;
import com.leonbon.tournaments.GameTitle;
import com.leonbon.tournaments.Tournament;
import com.leonbon.tournaments.TournamentEntry;
import com.leonbon.tournaments.TournamentEntryRepository;
import com.leonbon.tournaments.TournamentEntryStatus;
import com.leonbon.tournaments.TournamentEntryType;
import com.leonbon.tournaments.TournamentRepository;
import com.leonbon.tournaments.stats.BracketMatchStats;
import com.leonbon.tournaments.stats.BracketMatchStatsRepository;
import com.leonbon.tournaments.stats.MatchStatsFortniteRow;
import com.leonbon.tournaments.stats.MatchStatsMlbRow;
import com.leonbon.tournaments.stats.MatchStatsValorantRow;
import com.leonbon.teams.TeamRepository;
import com.leonbon.teams.TeamStatus;
import com.leonbon.users.User;
import com.leonbon.users.UserProfileService;
import com.leonbon.users.UserRepository;
import com.leonbon.web.NotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PlatformProfileService {
    private static final List<TournamentEntryStatus> APPROVED_ONLY = List.of(TournamentEntryStatus.APPROVED);

    private final UserRepository userRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final BracketMatchRepository bracketMatchRepository;
    private final BracketMatchStatsRepository bracketMatchStatsRepository;
    private final TeamRepository teamRepository;

    public PlatformProfileService(
            UserRepository userRepository,
            TournamentRepository tournamentRepository,
            TournamentEntryRepository tournamentEntryRepository,
            BracketMatchRepository bracketMatchRepository,
            BracketMatchStatsRepository bracketMatchStatsRepository,
            TeamRepository teamRepository
    ) {
        this.userRepository = userRepository;
        this.tournamentRepository = tournamentRepository;
        this.tournamentEntryRepository = tournamentEntryRepository;
        this.bracketMatchRepository = bracketMatchRepository;
        this.bracketMatchStatsRepository = bracketMatchStatsRepository;
        this.teamRepository = teamRepository;
    }

    public UserPlatformSnapshotResponse userSnapshot(String userId) {
        User u =
                userRepository.findById(userId).orElseThrow(() -> new NotFoundException("user not found"));
        List<GamePlatformRollupResponse> rollups = Arrays.stream(GameTitle.values())
                .map(g -> rollupForUser(g, userId))
                .toList();
        List<TeamAffiliationPublicResponse> aff =
                teamRepository.findByMemberUserIdsContainingAndStatusOrderByNameAsc(userId, TeamStatus.APPROVED).stream()
                        .map(t ->
                                new TeamAffiliationPublicResponse(
                                        t.getId(),
                                        t.getName(),
                                        t.getTag(),
                                        Objects.equals(t.getCaptainUserId(), userId),
                                        t.getRegionServer(),
                                        t.getLogoUrl()))
                        .toList();
        GameTitle pref = u.getPreferredGame();
        PublicPlayerSheetResponse sheet =
                new PublicPlayerSheetResponse(
                        u.getLeonPlayerNumber(),
                        publicFullNameAllow(u),
                        u.getCountry(),
                        UserProfileService.socialOf(u),
                        pref == null ? null : pref.name(),
                        u.getRankLabelByGame() == null || u.getRankLabelByGame().isEmpty()
                                ? Map.of()
                                : Map.copyOf(u.getRankLabelByGame()),
                        aff);
        return new UserPlatformSnapshotResponse(
                u.getId(),
                u.getUsername(),
                u.getNickname() == null ? "" : u.getNickname(),
                sheet,
                rollups);
    }

    private static String publicFullNameAllow(User u) {
        if (!u.isProfileShowFullName()) {
            return null;
        }
        String fn = u.getFullName();
        return fn == null || fn.isBlank() ? null : fn.trim();
    }

    public List<InternalLeaderboardRowResponse> leaderboard(GameTitle game, int limit) {
        int top = Math.max(1, Math.min(limit, 50));
        List<Tournament> tournaments = tournamentRepository.findByGame(game);
        if (tournaments.isEmpty()) {
            return List.of();
        }
        Set<String> tournamentIds =
                tournaments.stream().map(Tournament::getId).collect(Collectors.toSet());
        List<BracketMatch> done = bracketMatchRepository.findByTournamentIdInAndStatus(
                tournamentIds, BracketMatchStatus.COMPLETE);
        Set<String> entryIdsNeeded = new LinkedHashSet<>();
        for (BracketMatch m : done) {
            addIf(entryIdsNeeded, m.getEntryIdA());
            addIf(entryIdsNeeded, m.getEntryIdB());
            addIf(entryIdsNeeded, m.getWinnerEntryId());
        }
        Map<String, TournamentEntry> entriesById = loadEntries(entryIdsNeeded);
        Map<String, Integer> wins = new HashMap<>();
        for (BracketMatch m : done) {
            String winnerId = m.getWinnerEntryId();
            if (winnerId == null || winnerId.isBlank()) continue;
            TournamentEntry w = entriesById.get(winnerId);
            if (w == null) continue;
            for (String uid : participantUserIds(w)) {
                wins.merge(uid, 1, Integer::sum);
            }
        }
        List<Map.Entry<String, Integer>> sorted = wins.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();

        List<String> topIds = sorted.stream().map(Map.Entry::getKey).limit(top).toList();
        Map<String, User> usersBatch = loadUsers(topIds);

        List<InternalLeaderboardRowResponse> out = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, Integer> row : sorted) {
            User rowUser = usersBatch.get(row.getKey());
            if (rowUser == null) {
                rowUser = userRepository.findById(row.getKey()).orElse(null);
            }
            if (rowUser == null) continue;
            out.add(new InternalLeaderboardRowResponse(
                    rowUser.getId(),
                    rowUser.getUsername(),
                    rowUser.getNickname() == null ? "" : rowUser.getNickname(),
                    row.getValue()));
            count++;
            if (count >= top) break;
        }
        return out;
    }

    private GamePlatformRollupResponse rollupForUser(GameTitle game, String userId) {
        Map<String, Tournament> tournamentsById = tournamentRepository.findByGame(game).stream()
                .collect(Collectors.toMap(Tournament::getId, t -> t, (a, b) -> a, LinkedHashMap::new));
        if (tournamentsById.isEmpty()) {
            return emptyRollup(game);
        }

        List<TournamentEntry> playerRows =
                tournamentEntryRepository.findByPlayerIdAndStatusIn(userId, APPROVED_ONLY);
        List<TournamentEntry> rosterRows =
                tournamentEntryRepository.findByRosterUserIdAndStatusIn(userId, APPROVED_ONLY);

        Set<String> enteredTournamentIds = new LinkedHashSet<>();
        for (TournamentEntry e : playerRows) {
            if (tournamentsById.containsKey(e.getTournamentId())) {
                enteredTournamentIds.add(e.getTournamentId());
            }
        }
        for (TournamentEntry e : rosterRows) {
            if (tournamentsById.containsKey(e.getTournamentId())) {
                enteredTournamentIds.add(e.getTournamentId());
            }
        }

        List<BracketMatch> done = bracketMatchRepository.findByTournamentIdInAndStatus(
                tournamentsById.keySet(), BracketMatchStatus.COMPLETE);

        Set<String> loadIds = new LinkedHashSet<>();
        for (BracketMatch m : done) {
            addIf(loadIds, m.getEntryIdA());
            addIf(loadIds, m.getEntryIdB());
            addIf(loadIds, m.getWinnerEntryId());
        }
        Map<String, TournamentEntry> entriesById = loadEntries(loadIds);

        int wins = 0;
        int losses = 0;
        for (BracketMatch m : done) {
            TournamentEntry ea = idOrNull(entriesById, m.getEntryIdA());
            TournamentEntry eb = idOrNull(entriesById, m.getEntryIdB());
            boolean onA = ea != null && participantUserIds(ea).contains(userId);
            boolean onB = eb != null && participantUserIds(eb).contains(userId);
            if (!onA && !onB) continue;
            String wid = m.getWinnerEntryId();
            if (wid == null || wid.isBlank()) continue;
            boolean won = (onA && Objects.equals(wid, m.getEntryIdA())) || (onB && Objects.equals(wid, m.getEntryIdB()));
            if (won) wins++;
            else losses++;
        }

        List<BracketMatchStats> statsRows = bracketMatchStatsRepository.findByGame(game);
        ValAgg v = new ValAgg();
        FnAgg f = new FnAgg();
        MlbAgg mlb = new MlbAgg();
        for (BracketMatchStats doc : statsRows) {
            if (!tournamentsById.containsKey(doc.getTournamentId())) continue;
            for (MatchStatsValorantRow r : doc.getValorantRows()) {
                if (r == null || !Objects.equals(r.getUserId(), userId)) continue;
                v.add(r);
            }
            for (MatchStatsFortniteRow r : doc.getFortniteRows()) {
                if (r == null || !Objects.equals(r.getUserId(), userId)) continue;
                f.add(r);
            }
            for (MatchStatsMlbRow r : doc.getMlbRows()) {
                if (r == null || !Objects.equals(r.getUserId(), userId)) continue;
                mlb.add(r);
            }
        }

        int decidedBracket = wins + losses;
        Double winPctApprox =
                decidedBracket == 0 ? null : Math.round(wins * 10000.0 / decidedBracket) / 100.0;

        return new GamePlatformRollupResponse(
                game,
                enteredTournamentIds.size(),
                wins,
                losses,
                winPctApprox,
                v.samples() == 0 ? null : v.samples(),
                v.avgKda(),
                v.avgHs(),
                f.samples() == 0 ? null : f.samples(),
                f.avgKills(),
                f.avgKd(),
                f.avgPlacement(),
                f.royales() == 0 ? null : f.royales(),
                f.top10s() == 0 ? null : f.top10s(),
                f.dominantMode(),
                mlb.samples() == 0 ? null : mlb.samples(),
                mlb.avgBat(),
                mlb.avgHr(),
                mlb.avgIp(),
                mlb.avgEra(),
                mlb.avgRunsAllowed());
    }

    private static GamePlatformRollupResponse emptyRollup(GameTitle game) {
        return new GamePlatformRollupResponse(
                game,
                0,
                0,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static void addIf(Set<String> set, String id) {
        if (id != null && !id.isBlank()) set.add(id);
    }

    private static TournamentEntry idOrNull(Map<String, TournamentEntry> map, String id) {
        if (id == null || id.isBlank()) return null;
        return map.get(id);
    }

    private Map<String, TournamentEntry> loadEntries(Set<String> ids) {
        if (ids.isEmpty()) return Map.of();
        List<TournamentEntry> batch = tournamentEntryRepository.findAllById(ids);
        Map<String, TournamentEntry> map = new HashMap<>();
        for (TournamentEntry e : batch) {
            map.put(e.getId(), e);
        }
        return map;
    }

    private Map<String, User> loadUsers(List<String> ids) {
        if (ids.isEmpty()) return Map.of();
        List<User> batch = userRepository.findAllById(ids);
        Map<String, User> map = new HashMap<>();
        for (User u : batch) {
            map.put(u.getId(), u);
        }
        return map;
    }

    private static List<String> participantUserIds(TournamentEntry e) {
        if (e.getType() == TournamentEntryType.PLAYER) {
            return e.getPlayerId() == null || e.getPlayerId().isBlank()
                    ? List.of()
                    : List.of(e.getPlayerId());
        }
        List<String> roster = e.getSelectedRosterUserIds();
        if (roster == null || roster.isEmpty()) return List.of();
        return roster.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    private static final class ValAgg {
        int n;
        double kdaSum;
        int kdaCount;
        double hsSum;
        int hsCount;

        void add(MatchStatsValorantRow r) {
            n++;
            if (r.getKda() != null) {
                kdaSum += r.getKda();
                kdaCount++;
            }
            if (r.getHeadshotPct() != null) {
                hsSum += r.getHeadshotPct();
                hsCount++;
            }
        }

        int samples() {
            return n;
        }

        Double avgKda() {
            return kdaCount == 0 ? null : kdaSum / kdaCount;
        }

        Double avgHs() {
            return hsCount == 0 ? null : hsSum / hsCount;
        }
    }

    private static final class FnAgg {
        int n;
        double killSum;
        int killCount;
        double kdSum;
        int kdCount;
        double placeSum;
        int placeCount;
        int royales;
        int top10Matches;
        private final HashMap<String, Integer> modes = new HashMap<>();

        void add(MatchStatsFortniteRow r) {
            n++;
            if (r.getKills() != null) {
                killSum += r.getKills();
                killCount++;
            }
            int d = r.getDeaths() == null ? 0 : r.getDeaths();
            if (r.getKills() != null && d > 0) {
                kdSum += r.getKills() / (double) d;
                kdCount++;
            } else if (r.getKills() != null && d == 0) {
                kdSum += r.getKills();
                kdCount++;
            }
            if (r.getPlacement() != null) {
                placeSum += r.getPlacement();
                placeCount++;
                if (r.getPlacement() <= 10) top10Matches++;
                if (r.getPlacement() == 1) royales++;
            }
            if (r.getModePlayed() != null && !r.getModePlayed().isBlank()) {
                String mk = r.getModePlayed().trim();
                modes.merge(mk, 1, Integer::sum);
            }
        }

        int samples() {
            return n;
        }

        int royales() {
            return royales;
        }

        int top10s() {
            return top10Matches;
        }

        String dominantMode() {
            if (modes.isEmpty()) return null;
            return modes.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .findFirst()
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }

        Double avgKills() {
            return killCount == 0 ? null : killSum / killCount;
        }

        Double avgKd() {
            return kdCount == 0 ? null : kdSum / kdCount;
        }

        Double avgPlacement() {
            return placeCount == 0 ? null : placeSum / placeCount;
        }
    }

    private static final class MlbAgg {
        int n;
        double batSum;
        int batCount;
        double hrSum;
        int hrCount;
        double ipSum;
        int ipCount;
        double eraSum;
        int eraCount;
        double runsSum;
        int runsCount;

        void add(MatchStatsMlbRow r) {
            n++;
            if (r.getBattingAvgGame() != null) {
                batSum += r.getBattingAvgGame();
                batCount++;
            }
            if (r.getHomeRunsGame() != null) {
                hrSum += r.getHomeRunsGame();
                hrCount++;
            }
            if (r.getInningsPitchedGame() != null) {
                ipSum += r.getInningsPitchedGame();
                ipCount++;
            }
            if (r.getEraGame() != null) {
                eraSum += r.getEraGame();
                eraCount++;
            }
            if (r.getRunsAllowedGame() != null) {
                runsSum += r.getRunsAllowedGame();
                runsCount++;
            }
        }

        int samples() {
            return n;
        }

        Double avgBat() {
            return batCount == 0 ? null : batSum / batCount;
        }

        Double avgHr() {
            return hrCount == 0 ? null : hrSum / hrCount;
        }

        Double avgIp() {
            return ipCount == 0 ? null : ipSum / ipCount;
        }

        Double avgEra() {
            return eraCount == 0 ? null : eraSum / eraCount;
        }

        Double avgRunsAllowed() {
            return runsCount == 0 ? null : runsSum / runsCount;
        }
    }
}
