package com.leonbon.teams;

import com.leonbon.tournaments.BracketMatch;
import com.leonbon.tournaments.BracketMatchRepository;
import com.leonbon.tournaments.BracketMatchStatus;
import com.leonbon.tournaments.TournamentEntry;
import com.leonbon.tournaments.TournamentEntryRepository;
import com.leonbon.tournaments.TournamentEntryStatus;
import com.leonbon.teams.dto.TeamCompetitionSummaryResponse;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TeamCompetitionSummaryService {
    private static final List<TournamentEntryStatus> APPROVED = List.of(TournamentEntryStatus.APPROVED);

    private final TournamentEntryRepository tournamentEntryRepository;
    private final BracketMatchRepository bracketMatchRepository;

    public TeamCompetitionSummaryService(
            TournamentEntryRepository tournamentEntryRepository, BracketMatchRepository bracketMatchRepository) {
        this.tournamentEntryRepository = tournamentEntryRepository;
        this.bracketMatchRepository = bracketMatchRepository;
    }

    public TeamCompetitionSummaryResponse summarizeApprovedTeam(String teamId) {
        List<TournamentEntry> entries =
                tournamentEntryRepository.findByTeamIdAndStatusIn(teamId, APPROVED);
        if (entries.isEmpty()) {
            return new TeamCompetitionSummaryResponse(0, 0, 0, null);
        }
        Set<String> tournamentIds =
                entries.stream().map(TournamentEntry::getTournamentId).collect(Collectors.toSet());

        Map<String, Set<String>> entryIdsPerTournament = new HashMap<>();
        for (TournamentEntry e : entries) {
            entryIdsPerTournament
                    .computeIfAbsent(e.getTournamentId(), k -> new HashSet<>())
                    .add(e.getId());
        }

        List<BracketMatch> done =
                bracketMatchRepository.findByTournamentIdInAndStatus(tournamentIds, BracketMatchStatus.COMPLETE);

        int wins = 0;
        int losses = 0;
        for (BracketMatch m : done) {
            Set<String> validEntryIds =
                    entryIdsPerTournament.getOrDefault(m.getTournamentId(), Set.of());
            boolean onA =
                    m.getEntryIdA() != null && !m.getEntryIdA().isBlank() && validEntryIds.contains(m.getEntryIdA());
            boolean onB =
                    m.getEntryIdB() != null && !m.getEntryIdB().isBlank() && validEntryIds.contains(m.getEntryIdB());
            if (!onA && !onB) {
                continue;
            }
            String w = m.getWinnerEntryId();
            if (w == null || w.isBlank()) {
                continue;
            }
            boolean won = (onA && Objects.equals(w, m.getEntryIdA())) || (onB && Objects.equals(w, m.getEntryIdB()));
            if (won) {
                wins++;
            } else {
                losses++;
            }
        }

        int decided = wins + losses;
        Double pct =
                decided == 0 ? null : Math.round((wins * 10000.0 / decided)) / 100.0;

        return new TeamCompetitionSummaryResponse(tournamentIds.size(), wins, losses, pct);
    }
}
