package com.leonbon.tournaments;

import com.leonbon.auth.ConflictException;
import com.leonbon.auth.JwtPrincipal;
import com.leonbon.notifications.NotificationService;
import com.leonbon.teams.Team;
import com.leonbon.teams.TeamRepository;
import com.leonbon.teams.TeamStatus;
import com.leonbon.tournaments.dto.CreateTeamTournamentEntryRequest;
import com.leonbon.tournaments.dto.CreateTournamentRequest;
import com.leonbon.tournaments.dto.TournamentEntryResponse;
import com.leonbon.tournaments.dto.TournamentResponse;
import com.leonbon.users.User;
import com.leonbon.users.UserRepository;
import com.leonbon.users.UserRole;
import com.leonbon.users.UserStatus;
import com.leonbon.web.BadRequestException;
import com.leonbon.web.ForbiddenException;
import com.leonbon.web.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TournamentService {
    private static final List<TournamentEntryStatus> ACTIVE_ENTRY_STATUSES =
            List.of(TournamentEntryStatus.PENDING, TournamentEntryStatus.APPROVED);

    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private final int minRosterValorant;
    private final int minRosterFortnite;
    private final int minRosterMlb;

    public TournamentService(
            TournamentRepository tournamentRepository,
            TournamentEntryRepository tournamentEntryRepository,
            TeamRepository teamRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            @Value("${app.tournaments.minRoster.VALORANT}") int minRosterValorant,
            @Value("${app.tournaments.minRoster.FORTNITE}") int minRosterFortnite,
            @Value("${app.tournaments.minRoster.MLB}") int minRosterMlb
    ) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentEntryRepository = tournamentEntryRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.minRosterValorant = minRosterValorant;
        this.minRosterFortnite = minRosterFortnite;
        this.minRosterMlb = minRosterMlb;
    }

    private void assertDbAdmin(JwtPrincipal principal) {
        User u = userRepository.findById(principal.userId()).orElseThrow(() -> new NotFoundException("user not found"));
        UserRole role = u.getRole() == null ? UserRole.PLAYER : u.getRole();
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException("admin only");
        }
    }

    public TournamentResponse createTournamentAsAdmin(JwtPrincipal admin, CreateTournamentRequest req) {
        assertDbAdmin(admin);
        validateTournamentSchedule(
                req.getRegistrationStartAt(),
                req.getRegistrationEndAt(),
                req.getCompetitionStartAt(),
                req.getCompetitionEndAt()
        );

        Instant now = Instant.now();
        Tournament t = new Tournament();
        t.setName(req.getName().trim());
        t.setOrganizers(req.getOrganizers().trim());
        t.setGame(req.getGame());
        t.setFormat(req.getFormat());
        t.setLifecycleStatus(TournamentLifecycleStatus.REGISTRATION_SCHEDULED);
        t.setRegistrationManuallyOpened(false);
        t.setRegistrationStartAt(req.getRegistrationStartAt());
        t.setRegistrationEndAt(req.getRegistrationEndAt());
        t.setCompetitionStartAt(req.getCompetitionStartAt());
        t.setCompetitionEndAt(req.getCompetitionEndAt());
        t.setStreamUrl(trimToNull(req.getStreamUrl()));
        t.setRulesHtml(trimToBlankable(req.getRulesHtml()));
        t.setEligibilityNotes(trimToBlankable(req.getEligibilityNotes()));
        t.setPrizeNotes(trimToBlankable(req.getPrizeNotes()));
        if (req.getMaxApprovedParticipants() != null) {
            if (req.getMaxApprovedParticipants() < 1 || req.getMaxApprovedParticipants() > 256) {
                throw new BadRequestException("maxApprovedParticipants must be between 1 and 256");
            }
            t.setMaxApprovedParticipants(req.getMaxApprovedParticipants());
        }
        applyPlacementPrizeFields(t, req.getPrizeWinnerSlots(), req.getPrizeLeonCoinsByPlacement());
        t.setCreatedAt(now);
        t.setUpdatedAt(now);

        t = tournamentRepository.save(t);
        return toResponse(t);
    }

    public List<TournamentResponse> listPublicTournaments() {
        return tournamentRepository.findTop80ByOrderByCompetitionStartAtDesc().stream()
                .map(this::syncAndToResponse)
                .toList();
    }

    public List<TournamentResponse> listTournamentsForAdmin(JwtPrincipal admin) {
        assertDbAdmin(admin);
        return tournamentRepository.findTop200ByOrderByUpdatedAtDesc().stream()
                .map(this::syncAndToResponse)
                .toList();
    }

    public TournamentResponse getPublicTournament(String tournamentId) {
        Tournament t = tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        return syncAndToResponse(t);
    }

    private TournamentResponse syncAndToResponse(Tournament t) {
        if (TournamentRegistrationLifecycle.syncScheduledTransitions(t, Instant.now())) {
            t = tournamentRepository.save(t);
        }
        return toResponse(t);
    }

    public List<TournamentEntryResponse> listEntries(String tournamentId) {
        tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        List<TournamentEntry> rows = tournamentEntryRepository.findByTournamentIdOrderByCreatedAtAsc(tournamentId);
        Map<String, Team> teamsById = loadTeamsForEntries(rows);
        Map<String, User> usersById = loadUsersForEntries(rows);
        return rows.stream().map(e -> toEntryResponse(e, teamsById, usersById)).toList();
    }

    public TournamentEntryResponse approveEntryAsAdmin(JwtPrincipal admin, String tournamentId, String entryId) {
        assertDbAdmin(admin);
        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        TournamentEntry entry = tournamentEntryRepository.findById(entryId).orElseThrow(() -> new NotFoundException("entry not found"));
        if (!Objects.equals(entry.getTournamentId(), tournament.getId())) {
            throw new NotFoundException("entry not found");
        }
        if (entry.getStatus() != TournamentEntryStatus.PENDING) {
            throw new ConflictException("entry is not pending");
        }

        Integer cap = tournament.getMaxApprovedParticipants();
        if (cap != null) {
            long already = tournamentEntryRepository.countByTournamentIdAndStatus(tournament.getId(), TournamentEntryStatus.APPROVED);
            if (already >= cap) {
                throw new ConflictException("tournament approved entries cap reached (" + cap + ")");
            }
        }

        Instant now = Instant.now();
        entry.setStatus(TournamentEntryStatus.APPROVED);
        entry.setUpdatedAt(now);
        entry = tournamentEntryRepository.save(entry);
        notifyTournamentEntryDecision(tournament, entry, true);
        return toEntryResponse(entry);
    }

    public TournamentEntryResponse rejectEntryAsAdmin(JwtPrincipal admin, String tournamentId, String entryId) {
        assertDbAdmin(admin);
        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));
        TournamentEntry entry = tournamentEntryRepository.findById(entryId).orElseThrow(() -> new NotFoundException("entry not found"));
        if (!Objects.equals(entry.getTournamentId(), tournament.getId())) {
            throw new NotFoundException("entry not found");
        }
        if (entry.getStatus() != TournamentEntryStatus.PENDING) {
            throw new ConflictException("entry is not pending");
        }

        Instant now = Instant.now();
        entry.setStatus(TournamentEntryStatus.REJECTED);
        entry.setUpdatedAt(now);
        entry = tournamentEntryRepository.save(entry);
        notifyTournamentEntryDecision(tournament, entry, false);
        return toEntryResponse(entry);
    }

    public TournamentEntryResponse createTeamEntry(JwtPrincipal principal, String tournamentId, CreateTeamTournamentEntryRequest body) {
        User actor = getActiveUser(principal.userId());
        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));

        if (tournament.getGame() == GameTitle.MLB) {
            throw new BadRequestException("MLB tournaments use individual signup");
        }

        assertRegistrationOpen(tournament, Instant.now());

        Team team = teamRepository.findById(body.getTeamId()).orElseThrow(() -> new NotFoundException("team not found"));
        if (team.getStatus() != TeamStatus.APPROVED) {
            throw new ConflictException("team is not approved");
        }
        if (!Objects.equals(team.getCaptainUserId(), actor.getId())) {
            throw new ForbiddenException("captain only");
        }

        tournamentEntryRepository
                .findByTournamentIdAndTeamId(tournament.getId(), team.getId())
                .ifPresent(e -> {
                    throw new ConflictException("team already registered for this tournament");
                });

        LinkedHashSet<String> roster = new LinkedHashSet<>(body.getSelectedRosterUserIds());
        if (roster.isEmpty()) {
            throw new BadRequestException("roster is required");
        }

        int required = requiredRosterSize(tournament.getGame());
        if (roster.size() != required) {
            throw new BadRequestException("roster must select exactly " + required + " players for " + tournament.getGame());
        }

        List<String> members = team.getMemberUserIds() == null ? List.of() : team.getMemberUserIds();
        for (String userId : roster) {
            if (userId == null || userId.isBlank()) {
                throw new BadRequestException("invalid roster user id");
            }
            if (!members.contains(userId)) {
                throw new BadRequestException("roster must be a subset of team members");
            }
        }

        for (String userId : roster) {
            assertNoScheduleConflictForUser(userId, tournament, null);
        }
        assertNoScheduleConflictForTeam(team.getId(), tournament, null);

        Instant now = Instant.now();
        TournamentEntry entry = new TournamentEntry();
        entry.setTournamentId(tournament.getId());
        entry.setType(TournamentEntryType.TEAM);
        entry.setTeamId(team.getId());
        entry.setPlayerId(null);
        entry.setStatus(TournamentEntryStatus.PENDING);
        entry.setSelectedRosterUserIds(new ArrayList<>(roster));
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);

        entry = tournamentEntryRepository.save(entry);
        return toEntryResponse(entry);
    }

    public TournamentEntryResponse createMlbPlayerEntry(JwtPrincipal principal, String tournamentId) {
        User actor = getActiveUser(principal.userId());
        Tournament tournament = tournamentRepository.findById(tournamentId).orElseThrow(() -> new NotFoundException("tournament not found"));

        if (tournament.getGame() != GameTitle.MLB) {
            throw new BadRequestException("only MLB tournaments support individual signup");
        }

        assertRegistrationOpen(tournament, Instant.now());

        tournamentEntryRepository
                .findByTournamentIdAndPlayerId(tournament.getId(), actor.getId())
                .ifPresent(e -> {
                    throw new ConflictException("already registered for this tournament");
                });

        int required = minRosterMlb;
        if (required != 1) {
            // MVP expects 1v1 individual signup; keep config-driven but fail loudly if misconfigured.
            throw new BadRequestException("invalid MLB roster configuration");
        }

        assertNoScheduleConflictForUser(actor.getId(), tournament, null);

        Instant now = Instant.now();
        TournamentEntry entry = new TournamentEntry();
        entry.setTournamentId(tournament.getId());
        entry.setType(TournamentEntryType.PLAYER);
        entry.setTeamId(null);
        entry.setPlayerId(actor.getId());
        entry.setStatus(TournamentEntryStatus.PENDING);
        entry.setSelectedRosterUserIds(new ArrayList<>());
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);

        entry = tournamentEntryRepository.save(entry);
        return toEntryResponse(entry);
    }

    private void assertRegistrationOpen(Tournament tournament, Instant now) {
        if (TournamentRegistrationLifecycle.syncScheduledTransitions(tournament, now)) {
            tournamentRepository.save(tournament);
        }
        if (tournament.getLifecycleStatus() == TournamentLifecycleStatus.REGISTRATION_SCHEDULED) {
            throw new ConflictException("tournament registration has not opened yet");
        }
        if (!TournamentRegistrationLifecycle.isRegistrationAcceptingEntries(tournament, now)) {
            if (tournament.getLifecycleStatus() != TournamentLifecycleStatus.REGISTRATION_OPEN) {
                throw new ConflictException("tournament registration is not open");
            }
            throw new ConflictException("tournament is not in registration window");
        }
    }

    private void assertNoScheduleConflictForTeam(String teamId, Tournament newTournament, String ignoreEntryId) {
        List<TournamentEntry> entries = tournamentEntryRepository.findByTeamIdAndStatusIn(teamId, ACTIVE_ENTRY_STATUSES);
        assertNoScheduleConflictFromEntries(entries, newTournament, ignoreEntryId, "team has a conflicting tournament registration");
    }

    private void assertNoScheduleConflictForUser(String userId, Tournament newTournament, String ignoreEntryId) {
        List<TournamentEntry> byPlayer = tournamentEntryRepository.findByPlayerIdAndStatusIn(userId, ACTIVE_ENTRY_STATUSES);
        List<TournamentEntry> byRoster = tournamentEntryRepository.findByRosterUserIdAndStatusIn(userId, ACTIVE_ENTRY_STATUSES);

        List<TournamentEntry> combined = new ArrayList<>(byPlayer.size() + byRoster.size());
        combined.addAll(byPlayer);
        combined.addAll(byRoster);

        assertNoScheduleConflictFromEntries(combined, newTournament, ignoreEntryId, "player has a conflicting tournament registration");
    }

    private void assertNoScheduleConflictFromEntries(
            List<TournamentEntry> entries,
            Tournament newTournament,
            String ignoreEntryId,
            String message
    ) {
        Set<String> tournamentIds = new LinkedHashSet<>();
        for (TournamentEntry e : entries) {
            if (ignoreEntryId != null && Objects.equals(e.getId(), ignoreEntryId)) {
                continue;
            }
            if (e.getTournamentId() == null) {
                continue;
            }
            if (Objects.equals(e.getTournamentId(), newTournament.getId())) {
                continue;
            }
            tournamentIds.add(e.getTournamentId());
        }

        if (tournamentIds.isEmpty()) {
            return;
        }

        List<Tournament> others = tournamentRepository.findAllById(tournamentIds);
        for (Tournament other : others) {
            if (intervalsOverlap(
                    newTournament.getCompetitionStartAt(),
                    newTournament.getCompetitionEndAt(),
                    other.getCompetitionStartAt(),
                    other.getCompetitionEndAt()
            )) {
                throw new ConflictException(message);
            }
        }
    }

    private static boolean intervalsOverlap(Instant aStart, Instant aEnd, Instant bStart, Instant bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private static void validateTournamentSchedule(
            Instant registrationStartAt,
            Instant registrationEndAt,
            Instant competitionStartAt,
            Instant competitionEndAt
    ) {
        if (registrationStartAt == null
                || registrationEndAt == null
                || competitionStartAt == null
                || competitionEndAt == null) {
            throw new BadRequestException("schedule is required");
        }
        if (!registrationStartAt.isBefore(registrationEndAt)) {
            throw new BadRequestException("registrationStartAt must be before registrationEndAt");
        }
        if (!registrationEndAt.isBefore(competitionStartAt)) {
            throw new BadRequestException("registration must end before competition starts");
        }
        if (!competitionStartAt.isBefore(competitionEndAt)) {
            throw new BadRequestException("competitionStartAt must be before competitionEndAt");
        }
    }

    private int requiredRosterSize(GameTitle game) {
        return switch (game) {
            case VALORANT -> minRosterValorant;
            case FORTNITE -> minRosterFortnite;
            case MLB -> minRosterMlb;
        };
    }

    private User getActiveUser(String userId) {
        User u = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("user not found"));
        if (u.getStatus() != UserStatus.ACTIVE) {
            throw new ConflictException("user is not active");
        }
        return u;
    }

    private TournamentResponse toResponse(Tournament t) {
        return TournamentResponses.from(t);
    }

    /** Premios monetarios por puesto declarados por el admin en la creación. */
    private void applyPlacementPrizeFields(Tournament t, Integer slots, List<Long> rawAmounts) {
        if (slots == null) {
            t.setPrizeWinnerSlots(null);
            t.setPrizeLeonCoinsByPlacement(null);
            return;
        }
        if (slots < 0 || slots > 64) {
            throw new BadRequestException("prizeWinnerSlots must be between 0 and 64");
        }
        if (slots == 0) {
            t.setPrizeWinnerSlots(0);
            t.setPrizeLeonCoinsByPlacement(List.of());
            return;
        }
        List<Long> am = rawAmounts == null ? List.of() : rawAmounts;
        if (am.size() != slots) {
            throw new BadRequestException("prizeLeonCoinsByPlacement must have exactly " + slots + " amounts");
        }
        List<Long> normalized = new ArrayList<>();
        for (Long raw : am) {
            long v = raw == null ? 0 : raw.longValue();
            if (v < 0) {
                throw new BadRequestException("prize amounts must be non-negative");
            }
            if (v > 1_000_000_000_000L) {
                throw new BadRequestException("prize amount per placement exceeds limit");
            }
            normalized.add(v);
        }
        t.setPrizeWinnerSlots(slots);
        t.setPrizeLeonCoinsByPlacement(normalized);
    }

    private Map<String, Team> loadTeamsForEntries(List<TournamentEntry> rows) {
        Set<String> ids = new HashSet<>();
        for (TournamentEntry row : rows) {
            if (row.getType() == TournamentEntryType.TEAM && row.getTeamId() != null) {
                ids.add(row.getTeamId());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<String, Team> out = new HashMap<>();
        for (Team t : teamRepository.findAllById(ids)) {
            out.put(t.getId(), t);
        }
        return out;
    }

    private Map<String, User> loadUsersForEntries(List<TournamentEntry> rows) {
        Set<String> ids = new HashSet<>();
        for (TournamentEntry row : rows) {
            if (row.getType() == TournamentEntryType.PLAYER && row.getPlayerId() != null) {
                ids.add(row.getPlayerId());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<String, User> out = new HashMap<>();
        for (User u : userRepository.findAllById(ids)) {
            out.put(u.getId(), u);
        }
        return out;
    }

    private TournamentEntryResponse toEntryResponse(TournamentEntry e) {
        Map<String, Team> teams = new HashMap<>();
        Map<String, User> users = new HashMap<>();
        if (e.getType() == TournamentEntryType.TEAM && e.getTeamId() != null) {
            teamRepository.findById(e.getTeamId()).ifPresent(t -> teams.put(t.getId(), t));
        } else if (e.getType() == TournamentEntryType.PLAYER && e.getPlayerId() != null) {
            userRepository.findById(e.getPlayerId()).ifPresent(u -> users.put(u.getId(), u));
        }
        return toEntryResponse(e, teams, users);
    }

    private TournamentEntryResponse toEntryResponse(TournamentEntry e, Map<String, Team> teamsById, Map<String, User> usersById) {
        String teamName = null;
        String teamTag = null;
        String playerUsername = null;
        if (e.getType() == TournamentEntryType.TEAM && e.getTeamId() != null) {
            Team t = teamsById.get(e.getTeamId());
            if (t != null) {
                teamName = t.getName();
                teamTag = t.getTag();
            }
        } else if (e.getType() == TournamentEntryType.PLAYER && e.getPlayerId() != null) {
            User u = usersById.get(e.getPlayerId());
            if (u != null) {
                playerUsername = u.getUsername();
            }
        }
        return new TournamentEntryResponse(
                e.getId(),
                e.getTournamentId(),
                e.getType(),
                e.getTeamId(),
                e.getPlayerId(),
                e.getStatus(),
                e.getSelectedRosterUserIds() == null ? List.of() : List.copyOf(e.getSelectedRosterUserIds()),
                e.getCreatedAt(),
                teamName,
                teamTag,
                playerUsername
        );
    }

    private void notifyTournamentEntryDecision(Tournament tournament, TournamentEntry entry, boolean approved) {
        LinkedHashSet<String> recipients = new LinkedHashSet<>();
        TournamentEntryType type = entry.getType() == null ? TournamentEntryType.TEAM : entry.getType();
        if (type == TournamentEntryType.PLAYER) {
            String pid = entry.getPlayerId();
            if (pid != null && !pid.isBlank()) {
                recipients.add(pid);
            }
        } else {
            if (entry.getTeamId() != null) {
                teamRepository.findById(entry.getTeamId()).ifPresent(team -> {
                    if (team.getCaptainUserId() != null && !team.getCaptainUserId().isBlank()) {
                        recipients.add(team.getCaptainUserId());
                    }
                });
            }
            List<String> roster = entry.getSelectedRosterUserIds();
            if (roster != null) {
                for (String uid : roster) {
                    if (uid != null && !uid.trim().isEmpty()) recipients.add(uid.trim());
                }
            }
        }
        String tn = tournament.getName() == null ? "Torneo" : tournament.getName();
        String tid = tournament.getId();
        String eid = entry.getId();
        for (String uid : recipients) {
            if (approved) {
                notificationService.publishTournamentEntryApproved(uid, tn, tid, eid);
            } else {
                notificationService.publishTournamentEntryRejected(uid, tn, tid, eid);
            }
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Texto opcional almacenado como cadena vacía en BD en lugar de null. */
    private static String trimToBlankable(String s) {
        return s == null ? "" : s.trim();
    }
}
