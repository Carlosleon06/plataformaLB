package com.leonbon.teams;

import com.leonbon.teams.dto.TeamCollectiveBracketStatsResponse;
import com.leonbon.teams.dto.TeamCollectiveBracketStatsResponse.TeamFortniteAgg;
import com.leonbon.teams.dto.TeamCollectiveBracketStatsResponse.TeamMlbAgg;
import com.leonbon.teams.dto.TeamCollectiveBracketStatsResponse.TeamValorantAgg;
import com.leonbon.tournaments.BracketMatch;
import com.leonbon.tournaments.BracketMatchRepository;
import com.leonbon.tournaments.BracketMatchStatus;
import com.leonbon.tournaments.GameTitle;
import com.leonbon.tournaments.TournamentEntry;
import com.leonbon.tournaments.TournamentEntryRepository;
import com.leonbon.tournaments.TournamentEntryStatus;
import com.leonbon.tournaments.stats.BracketMatchStats;
import com.leonbon.tournaments.stats.BracketMatchStatsRepository;
import com.leonbon.tournaments.stats.MatchStatsFortniteRow;
import com.leonbon.tournaments.stats.MatchStatsMlbRow;
import com.leonbon.tournaments.stats.MatchStatsValorantRow;
import com.leonbon.web.NotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TeamCollectiveBracketStatsService {
    private static final List<TournamentEntryStatus> APPROVED = List.of(TournamentEntryStatus.APPROVED);

    private final TeamRepository teamRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final BracketMatchStatsRepository bracketMatchStatsRepository;
    private final BracketMatchRepository bracketMatchRepository;

    public TeamCollectiveBracketStatsService(
            TeamRepository teamRepository,
            TournamentEntryRepository tournamentEntryRepository,
            BracketMatchStatsRepository bracketMatchStatsRepository,
            BracketMatchRepository bracketMatchRepository) {
        this.teamRepository = teamRepository;
        this.tournamentEntryRepository = tournamentEntryRepository;
        this.bracketMatchStatsRepository = bracketMatchStatsRepository;
        this.bracketMatchRepository = bracketMatchRepository;
    }

    public TeamCollectiveBracketStatsResponse summarize(String teamId) {
        Team team =
                teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        if (team.getStatus() != TeamStatus.APPROVED) {
            return emptyForVisibility();
        }

        List<TournamentEntry> entries =
                tournamentEntryRepository.findByTeamIdAndStatusIn(teamId, APPROVED);
        if (entries.isEmpty()) {
            return new TeamCollectiveBracketStatsResponse(
                    0,
                    0,
                    zeroValorant(),
                    zeroFortnite(),
                    zeroMlb());
        }

        Set<String> staff = new HashSet<>();
        team.getMemberUserIds().stream().filter(Objects::nonNull).forEach(staff::add);
        String cap = team.getCaptainUserId();
        if (cap != null && !cap.isBlank()) {
            staff.add(cap);
        }

        Set<String> tournamentIds =
                entries.stream().map(TournamentEntry::getTournamentId).collect(Collectors.toSet());

        Map<String, Set<String>> entryIdsPerTournament = new HashMap<>();
        for (TournamentEntry e : entries) {
            entryIdsPerTournament
                    .computeIfAbsent(e.getTournamentId(), k -> new HashSet<>())
                    .add(e.getId());
        }

        List<BracketMatchStats> statsRows = bracketMatchStatsRepository.findByTournamentIdIn(tournamentIds);

        int matchesAttributed = 0;

        double vKdaSum = 0;
        int vKdaCount = 0;
        long vKills = 0;
        long vDeaths = 0;
        long vAssists = 0;
        double vHsSum = 0;
        int vHsCount = 0;
        int vRows = 0;

        long fKills = 0;
        long fDeaths = 0;
        double fPlaceSum = 0;
        int fPlaceCount = 0;
        int fRows = 0;

        double mAvgSum = 0;
        int mAvgCount = 0;
        long mHr = 0;
        double mIpSum = 0;
        int mIpCount = 0;
        double mEraSum = 0;
        int mEraCount = 0;
        int mRows = 0;

        for (BracketMatchStats s : statsRows) {
            BracketMatch m = bracketMatchRepository.findById(s.getMatchId()).orElse(null);
            if (m == null || m.getStatus() != BracketMatchStatus.COMPLETE) {
                continue;
            }
            Set<String> teamEntryIds = entryIdsPerTournament.getOrDefault(m.getTournamentId(), Set.of());
            boolean onA = m.getEntryIdA() != null && teamEntryIds.contains(m.getEntryIdA());
            boolean onB = m.getEntryIdB() != null && teamEntryIds.contains(m.getEntryIdB());
            if (!onA && !onB) {
                continue;
            }
            matchesAttributed++;

            GameTitle g = s.getGame();
            if (g == GameTitle.VALORANT) {
                for (MatchStatsValorantRow r : s.getValorantRows()) {
                    if (r.getUserId() == null || !staff.contains(r.getUserId())) {
                        continue;
                    }
                    vRows++;
                    if (r.getKda() != null) {
                        vKdaSum += r.getKda();
                        vKdaCount++;
                    }
                    if (r.getKills() != null) vKills += r.getKills();
                    if (r.getDeaths() != null) vDeaths += r.getDeaths();
                    if (r.getAssists() != null) vAssists += r.getAssists();
                    if (r.getHeadshotPct() != null) {
                        vHsSum += r.getHeadshotPct();
                        vHsCount++;
                    }
                }
            } else if (g == GameTitle.FORTNITE) {
                for (MatchStatsFortniteRow r : s.getFortniteRows()) {
                    if (r.getUserId() == null || !staff.contains(r.getUserId())) {
                        continue;
                    }
                    fRows++;
                    if (r.getKills() != null) fKills += r.getKills();
                    if (r.getDeaths() != null) fDeaths += r.getDeaths();
                    if (r.getPlacement() != null) {
                        fPlaceSum += r.getPlacement();
                        fPlaceCount++;
                    }
                }
            } else if (g == GameTitle.MLB) {
                for (MatchStatsMlbRow r : s.getMlbRows()) {
                    if (r.getUserId() == null || !staff.contains(r.getUserId())) {
                        continue;
                    }
                    mRows++;
                    if (r.getBattingAvgGame() != null) {
                        mAvgSum += r.getBattingAvgGame();
                        mAvgCount++;
                    }
                    if (r.getHomeRunsGame() != null) mHr += r.getHomeRunsGame();
                    if (r.getInningsPitchedGame() != null) {
                        mIpSum += r.getInningsPitchedGame();
                        mIpCount++;
                    }
                    if (r.getEraGame() != null) {
                        mEraSum += r.getEraGame();
                        mEraCount++;
                    }
                }
            }
        }

        TeamValorantAgg vAgg =
                new TeamValorantAgg(
                        vRows,
                        vKdaCount == 0 ? null : Math.round((vKdaSum / vKdaCount) * 100.0) / 100.0,
                        vKills,
                        vDeaths,
                        vAssists,
                        vHsCount == 0 ? null : Math.round((vHsSum / vHsCount) * 100.0) / 100.0);

        Double fKd = fDeaths <= 0 ? (fKills > 0 ? (double) fKills : null) : Math.round(((double) fKills / fDeaths) * 100.0) / 100.0;

        TeamFortniteAgg fAgg =
                new TeamFortniteAgg(
                        fRows,
                        fKd,
                        fKills,
                        fDeaths,
                        fPlaceCount == 0 ? null : Math.round((fPlaceSum / fPlaceCount) * 100.0) / 100.0);

        TeamMlbAgg mAgg =
                new TeamMlbAgg(
                        mRows,
                        mAvgCount == 0 ? null : Math.round((mAvgSum / mAvgCount) * 1000.0) / 1000.0,
                        mHr,
                        mIpCount == 0 ? null : Math.round((mIpSum / mIpCount) * 100.0) / 100.0,
                        mEraCount == 0 ? null : Math.round((mEraSum / mEraCount) * 100.0) / 100.0);

        return new TeamCollectiveBracketStatsResponse(tournamentIds.size(), matchesAttributed, vAgg, fAgg, mAgg);
    }

    private TeamCollectiveBracketStatsResponse emptyForVisibility() {
        return new TeamCollectiveBracketStatsResponse(0, 0, zeroValorant(), zeroFortnite(), zeroMlb());
    }

    private static TeamValorantAgg zeroValorant() {
        return new TeamValorantAgg(0, null, 0L, 0L, 0L, null);
    }

    private static TeamFortniteAgg zeroFortnite() {
        return new TeamFortniteAgg(0, null, 0L, 0L, null);
    }

    private static TeamMlbAgg zeroMlb() {
        return new TeamMlbAgg(0, null, 0L, null, null);
    }
}
