package com.leonbon.teams;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.files.LocalLogoStorageService;
import com.leonbon.teams.dto.CaptainTeamSummaryResponse;
import com.leonbon.teams.dto.PendingTeamAdminRow;
import com.leonbon.teams.dto.CreateTeamRequest;
import com.leonbon.teams.dto.JoinRequestResponse;
import com.leonbon.teams.dto.TeamCaptainViewResponse;
import com.leonbon.teams.dto.PatchCaptainTeamPresenceRequest;
import com.leonbon.teams.dto.TeamCompetitionSummaryResponse;
import com.leonbon.teams.dto.TeamPublicResponse;
import com.leonbon.auth.ConflictException;
import com.leonbon.tournaments.TournamentEntry;
import com.leonbon.tournaments.TournamentEntryRepository;
import com.leonbon.tournaments.TournamentEntryStatus;
import com.leonbon.users.User;
import com.leonbon.users.UserRepository;
import com.leonbon.notifications.NotificationService;
import com.leonbon.users.UserRole;
import com.leonbon.users.UserStatus;
import com.leonbon.web.BadRequestException;
import com.leonbon.web.ForbiddenException;
import com.leonbon.web.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TeamService {
    private static final List<TournamentEntryStatus> TOURNAMENT_REGISTRATION_LOCK_STATUSES =
            List.of(TournamentEntryStatus.PENDING, TournamentEntryStatus.APPROVED);

    private final TeamRepository teamRepository;
    private final TeamJoinRequestRepository joinRequestRepository;
    private final UserRepository userRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final NotificationService notificationService;
    private final String defaultLogoUrl;
    private final LocalLogoStorageService logoStorageService;
    private final TeamCompetitionSummaryService teamCompetitionSummaryService;

    public TeamService(
            TeamRepository teamRepository,
            TeamJoinRequestRepository joinRequestRepository,
            UserRepository userRepository,
            TournamentEntryRepository tournamentEntryRepository,
            NotificationService notificationService,
            TeamCompetitionSummaryService teamCompetitionSummaryService,
            @Value("${app.teams.defaultLogoUrl}") String defaultLogoUrl,
            LocalLogoStorageService logoStorageService
    ) {
        this.teamRepository = teamRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.userRepository = userRepository;
        this.tournamentEntryRepository = tournamentEntryRepository;
        this.notificationService = notificationService;
        this.teamCompetitionSummaryService = teamCompetitionSummaryService;
        this.defaultLogoUrl = defaultLogoUrl;
        this.logoStorageService = logoStorageService;
    }

    public TeamPublicResponse createTeam(JwtPrincipal principal, CreateTeamRequest req) {
        User actor = getActiveUser(principal.userId());

        String name = req.getName().trim();
        String tag = req.getTag().trim();
        String region = req.getRegionServer().trim();

        if (teamRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("team name already exists");
        }

        Team team = new Team();
        team.setName(name);
        team.setTag(tag);
        team.setRegionServer(region);
        team.setLogoUrl(defaultLogoUrl);
        team.setStatus(TeamStatus.PENDING);
        team.setCaptainUserId(actor.getId());
        team.setMemberUserIds(new ArrayList<>(List.of(actor.getId())));
        team.setCoachUserIds(new ArrayList<>());
        team.setSponsorLines(sanitizeSponsorLines(req.getSponsorLines()));
        team.setCanonicalStreamUrl(trimToNullCommercial(req.getCanonicalStreamUrl()));
        team.setCreatedAt(Instant.now());
        team.setUpdatedAt(Instant.now());

        try {
            team = teamRepository.save(team);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("team name already exists");
        }

        return toPublic(team, null);
    }

    public TeamCaptainViewResponse patchCaptainCommercialFields(
            JwtPrincipal principal, String teamId, PatchCaptainTeamPresenceRequest body) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        assertCaptain(team, principal.userId());
        if (team.getStatus() != TeamStatus.APPROVED && team.getStatus() != TeamStatus.PENDING) {
            throw new ConflictException("team cannot be edited in this status");
        }
        if (body.getSponsorLines() != null) {
            team.setSponsorLines(sanitizeSponsorLines(body.getSponsorLines()));
        }
        if (body.getCanonicalStreamUrl() != null) {
            team.setCanonicalStreamUrl(trimToNullCommercial(body.getCanonicalStreamUrl()));
        }
        team.setUpdatedAt(Instant.now());
        teamRepository.save(team);
        return toCaptainView(teamRepository.findById(teamId).orElseThrow());
    }

    public List<TeamPublicResponse> listApprovedTeams() {
        return teamRepository.findTop20ByStatusOrderByCreatedAtDesc(TeamStatus.APPROVED).stream()
                .map(t -> toPublic(t, null))
                .toList();
    }

    public List<CaptainTeamSummaryResponse> listMyApprovedCaptainTeams(JwtPrincipal principal) {
        return teamRepository.findByCaptainUserIdAndStatusOrderByNameAsc(principal.userId(), TeamStatus.APPROVED).stream()
                .map(t -> new CaptainTeamSummaryResponse(
                        t.getId(),
                        t.getName(),
                        t.getTag(),
                        t.getRegionServer(),
                        t.getLogoUrl(),
                        t.getMemberUserIds() == null ? List.of() : List.copyOf(t.getMemberUserIds())
                ))
                .toList();
    }

    public void assertDbAdmin(JwtPrincipal principal) {
        User u = userRepository.findById(principal.userId()).orElseThrow(() -> new NotFoundException("user not found"));
        UserRole role = u.getRole() == null ? UserRole.PLAYER : u.getRole();
        if (role != UserRole.ADMIN) {
            throw new ForbiddenException("admin only");
        }
    }

    public List<PendingTeamAdminRow> listPendingTeamsForAdmin(JwtPrincipal admin) {
        assertDbAdmin(admin);
        return teamRepository.findTop100ByStatusOrderByCreatedAtAsc(TeamStatus.PENDING).stream()
                .map(t -> {
                    User cap = userRepository.findById(t.getCaptainUserId()).orElseThrow();
                    int members = t.getMemberUserIds() == null ? 0 : t.getMemberUserIds().size();
                    return new PendingTeamAdminRow(
                            t.getId(),
                            t.getName(),
                            t.getTag(),
                            t.getStatus(),
                            t.getRegionServer(),
                            cap.getUsername(),
                            members,
                            t.getCreatedAt()
                    );
                })
                .toList();
    }

    public List<TeamPublicResponse> searchApprovedTeams(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            throw new BadRequestException("query too short");
        }
        return teamRepository.findTop50ByStatusAndNameContainingIgnoreCaseOrderByNameAsc(TeamStatus.APPROVED, q).stream()
                .map(t -> toPublic(t, null))
                .toList();
    }

    public TeamPublicResponse getPublicTeam(String teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        if (team.getStatus() != TeamStatus.APPROVED) {
            throw new NotFoundException("team not found");
        }
        TeamCompetitionSummaryResponse comp = teamCompetitionSummaryService.summarizeApprovedTeam(team.getId());
        return toPublic(team, comp);
    }

    public Object getTeamForViewer(JwtPrincipal principal, String teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        if (isAdmin(principal)) {
            return toCaptainView(team);
        }
        if (team.getStatus() == TeamStatus.APPROVED) {
            boolean isMember = team.getMemberUserIds() != null && team.getMemberUserIds().contains(principal.userId());
            if (isMember) {
                return toCaptainView(team);
            }
            return toPublic(team, teamCompetitionSummaryService.summarizeApprovedTeam(team.getId()));
        }

        // Non-approved teams are visible to roster members (captain is always a member)
        boolean isMember = team.getMemberUserIds() != null && team.getMemberUserIds().contains(principal.userId());
        if (isMember) {
            return toCaptainView(team);
        }

        throw new NotFoundException("team not found");
    }

    public JoinRequestResponse requestJoin(JwtPrincipal principal, String teamId) {
        User actor = getActiveUser(principal.userId());
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        assertTeamJoinable(team);
        if (team.getStatus() != TeamStatus.APPROVED) {
            throw new ConflictException("team is not accepting join requests");
        }
        if (Objects.equals(team.getCaptainUserId(), actor.getId())) {
            throw new ConflictException("captain cannot request to join own team");
        }
        if (team.getMemberUserIds().contains(actor.getId())) {
            throw new ConflictException("already a member");
        }

        joinRequestRepository
                .findByTeamIdAndRequesterUserIdAndStatus(team.getId(), actor.getId(), JoinRequestStatus.PENDING)
                .ifPresent(r -> {
                    throw new ConflictException("join request already pending");
                });

        TeamJoinRequest req = new TeamJoinRequest();
        req.setTeamId(team.getId());
        req.setRequesterUserId(actor.getId());
        req.setStatus(JoinRequestStatus.PENDING);
        req.setCreatedAt(Instant.now());
        req.setUpdatedAt(Instant.now());
        req = joinRequestRepository.save(req);

        User requester = actor;
        return toJoinRequest(req, requester.getUsername());
    }

    public List<JoinRequestResponse> listPendingJoinRequests(JwtPrincipal principal, String teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        assertCaptain(team, principal.userId());
        if (team.getStatus() != TeamStatus.APPROVED) {
            return List.of();
        }

        return joinRequestRepository.findByTeamIdAndStatusOrderByCreatedAtAsc(team.getId(), JoinRequestStatus.PENDING).stream()
                .map(r -> {
                    User u = userRepository.findById(r.getRequesterUserId()).orElseThrow();
                    return toJoinRequest(r, u.getUsername());
                })
                .toList();
    }

    public JoinRequestResponse respondJoinRequest(JwtPrincipal principal, String teamId, String requestId, JoinRequestStatus next) {
        if (next != JoinRequestStatus.ACCEPTED && next != JoinRequestStatus.REJECTED) {
            throw new BadRequestException("invalid status");
        }

        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        assertTeamRosterMutable(team);
        assertCaptain(team, principal.userId());

        TeamJoinRequest req = joinRequestRepository.findById(requestId).orElseThrow(() -> new NotFoundException("request not found"));
        if (!Objects.equals(req.getTeamId(), team.getId())) {
            throw new NotFoundException("request not found");
        }
        if (req.getStatus() != JoinRequestStatus.PENDING) {
            throw new ConflictException("request is not pending");
        }

        Instant now = Instant.now();
        req.setStatus(next);
        req.setUpdatedAt(now);
        joinRequestRepository.save(req);

        if (next == JoinRequestStatus.ACCEPTED) {
            User joiner = userRepository.findById(req.getRequesterUserId()).orElseThrow(() -> new NotFoundException("user not found"));
            if (joiner.getStatus() != UserStatus.ACTIVE) {
                throw new ConflictException("user is not active");
            }
            LinkedHashSet<String> members = new LinkedHashSet<>(team.getMemberUserIds());
            members.add(joiner.getId());
            team.setMemberUserIds(new ArrayList<>(members));
            team.setUpdatedAt(now);
            teamRepository.save(team);
            notificationService.publishTeamJoinAccepted(joiner.getId(), team.getId(), team.getName());
        } else {
            notificationService.publishTeamJoinRejected(req.getRequesterUserId(), team.getId(), team.getName());
        }

        User requester = userRepository.findById(req.getRequesterUserId()).orElseThrow();
        return toJoinRequest(req, requester.getUsername());
    }

    public TeamCaptainViewResponse delegateCaptain(JwtPrincipal principal, String teamId, String newCaptainUserId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        assertTeamRosterMutable(team);
        assertCaptain(team, principal.userId());

        if (!team.getMemberUserIds().contains(newCaptainUserId)) {
            throw new BadRequestException("new captain must be a team member");
        }
        if (Objects.equals(team.getCaptainUserId(), newCaptainUserId)) {
            return toCaptainView(team);
        }

        // captain cannot be coach
        List<String> coaches = new ArrayList<>(team.getCoachUserIds());
        coaches.remove(newCaptainUserId);
        if (coaches.size() > 3) {
            // should never happen, but keep safe
            throw new BadRequestException("too many coaches");
        }

        team.setCaptainUserId(newCaptainUserId);
        team.setCoachUserIds(coaches);
        team.setUpdatedAt(Instant.now());
        teamRepository.save(team);

        team = teamRepository.findById(teamId).orElseThrow();
        return toCaptainView(team);
    }

    public TeamCaptainViewResponse addCoach(JwtPrincipal principal, String teamId, String coachUserId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        assertTeamRosterMutable(team);
        assertCaptain(team, principal.userId());
        if (team.getStatus() != TeamStatus.APPROVED) {
            throw new ConflictException("team is not approved yet");
        }
        if (Objects.equals(team.getCaptainUserId(), coachUserId)) {
            throw new BadRequestException("captain cannot be coach");
        }
        if (!team.getMemberUserIds().contains(coachUserId)) {
            throw new BadRequestException("coach must be a team member");
        }

        LinkedHashSet<String> coaches = new LinkedHashSet<>(team.getCoachUserIds());
        coaches.add(coachUserId);
        if (coaches.size() > 3) {
            throw new BadRequestException("max 3 coaches");
        }
        team.setCoachUserIds(new ArrayList<>(coaches));
        team.setUpdatedAt(Instant.now());
        teamRepository.save(team);

        team = teamRepository.findById(teamId).orElseThrow();
        return toCaptainView(team);
    }

    public TeamCaptainViewResponse removeCoach(JwtPrincipal principal, String teamId, String coachUserId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        assertTeamRosterMutable(team);
        assertCaptain(team, principal.userId());

        List<String> coaches = new ArrayList<>(team.getCoachUserIds());
        coaches.remove(coachUserId);
        team.setCoachUserIds(coaches);
        team.setUpdatedAt(Instant.now());
        teamRepository.save(team);

        team = teamRepository.findById(teamId).orElseThrow();
        return toCaptainView(team);
    }

    public void approveTeamAsAdmin(JwtPrincipal admin, String teamId) {
        assertDbAdmin(admin);
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        if (team.getStatus() != TeamStatus.PENDING) {
            throw new ConflictException("team is not pending");
        }
        team.setStatus(TeamStatus.APPROVED);
        team.setUpdatedAt(Instant.now());
        teamRepository.save(team);
    }

    public void rejectTeamAsAdmin(JwtPrincipal admin, String teamId) {
        assertDbAdmin(admin);
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        if (team.getStatus() != TeamStatus.PENDING) {
            throw new ConflictException("team is not pending");
        }
        team.setStatus(TeamStatus.REJECTED);
        team.setUpdatedAt(Instant.now());
        teamRepository.save(team);
    }

    public void suspendTeamAsAdmin(JwtPrincipal admin, String teamId) {
        assertDbAdmin(admin);
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        team.setStatus(TeamStatus.SUSPENDED);
        team.setUpdatedAt(Instant.now());
        teamRepository.save(team);
    }

    public TeamCaptainViewResponse uploadLogo(JwtPrincipal principal, String teamId, MultipartFile file) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        assertTeamLogoMutable(team);
        assertCaptain(team, principal.userId());

        String url = logoStorageService.storeLogo(file);
        Instant now = Instant.now();
        team.setLogoUrl(url);
        team.setUpdatedAt(now);
        teamRepository.save(team);

        return toCaptainView(teamRepository.findById(teamId).orElseThrow());
    }

    public TeamCaptainViewResponse resetLogoCaptain(JwtPrincipal principal, String teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        assertTeamLogoMutable(team);
        assertCaptain(team, principal.userId());

        Instant now = Instant.now();
        team.setLogoUrl(defaultLogoUrl);
        team.setUpdatedAt(now);
        teamRepository.save(team);

        return toCaptainView(teamRepository.findById(teamId).orElseThrow());
    }

    public void resetLogoAdmin(JwtPrincipal admin, String teamId) {
        assertDbAdmin(admin);
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        Instant now = Instant.now();
        team.setLogoUrl(defaultLogoUrl);
        team.setUpdatedAt(now);
        teamRepository.save(team);
    }

    public TeamCaptainViewResponse leaveTeam(JwtPrincipal principal, String teamId) {
        User actor = getActiveUser(principal.userId());
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        assertTeamRosterMutable(team);

        if (Objects.equals(team.getCaptainUserId(), actor.getId())) {
            int members = team.getMemberUserIds() == null ? 0 : team.getMemberUserIds().size();
            if (members > 1) {
                throw new ConflictException("captain must delegate before leaving");
            }
            return disbandTeam(team, Instant.now());
        }

        assertMemberNotListedInLockedTournamentRoster(team.getId(), actor.getId());

        LinkedHashSet<String> members = new LinkedHashSet<>(team.getMemberUserIds());
        if (!members.remove(actor.getId())) {
            throw new ConflictException("not a team member");
        }

        List<String> coaches = new ArrayList<>(team.getCoachUserIds());
        coaches.remove(actor.getId());

        team.setMemberUserIds(new ArrayList<>(members));
        team.setCoachUserIds(coaches);
        team.setUpdatedAt(Instant.now());
        teamRepository.save(team);

        return toCaptainView(teamRepository.findById(teamId).orElseThrow());
    }

    public TeamCaptainViewResponse disbandTeamIfSoleCaptain(JwtPrincipal principal, String teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        assertTeamRosterMutable(team);
        assertCaptain(team, principal.userId());

        int members = team.getMemberUserIds() == null ? 0 : team.getMemberUserIds().size();
        if (members != 1) {
            throw new ConflictException("team can only be disbanded when captain is the sole member");
        }

        return disbandTeam(team, Instant.now());
    }

    private TeamCaptainViewResponse disbandTeam(Team team, Instant now) {
        assertTeamHasNoBlockingTournamentEntry(team.getId());

        team.setStatus(TeamStatus.DISBANDED);
        team.setUpdatedAt(now);
        teamRepository.save(team);
        rejectAllPendingJoinRequests(team.getId(), now);
        return toCaptainView(teamRepository.findById(team.getId()).orElseThrow());
    }

    private void rejectAllPendingJoinRequests(String teamId, Instant now) {
        List<TeamJoinRequest> pending = joinRequestRepository.findByTeamIdAndStatusOrderByCreatedAtAsc(teamId, JoinRequestStatus.PENDING);
        for (TeamJoinRequest r : pending) {
            r.setStatus(JoinRequestStatus.REJECTED);
            r.setUpdatedAt(now);
            joinRequestRepository.save(r);
        }
    }

    private void assertTeamJoinable(Team team) {
        if (team.getStatus() == TeamStatus.DISBANDED) {
            throw new ConflictException("team is disbanded");
        }
        if (team.getStatus() == TeamStatus.SUSPENDED) {
            throw new ConflictException("team is suspended");
        }
    }

    private void assertTeamRosterMutable(Team team) {
        assertTeamJoinable(team);
        if (team.getStatus() == TeamStatus.REJECTED) {
            throw new ConflictException("team is rejected");
        }
    }

    /** Equipo no puede deshacer roster (disolver) si está inscripto — aun pendiente — en algún torneo. */
    private void assertTeamHasNoBlockingTournamentEntry(String teamId) {
        List<TournamentEntry> rows = tournamentEntryRepository.findByTeamIdAndStatusIn(teamId, TOURNAMENT_REGISTRATION_LOCK_STATUSES);
        if (!rows.isEmpty()) {
            throw new ConflictException(
                    "the team cannot be disbanded while it has a tournament registration pending or approved"
            );
        }
    }

    /** Jugadores listados para un torneo (roster declarado) no pueden abandonar hasta que liberen la inscripción. */
    private void assertMemberNotListedInLockedTournamentRoster(String teamId, String memberUserId) {
        List<TournamentEntry> rows = tournamentEntryRepository.findByTeamIdAndSelectedRosterUserIdsContainingAndStatusIn(
                teamId,
                memberUserId,
                TOURNAMENT_REGISTRATION_LOCK_STATUSES
        );
        if (!rows.isEmpty()) {
            throw new ConflictException(
                    "you are listed as a roster player on a tournament entry (pending or approved) and cannot leave the team yet"
            );
        }
    }

    private void assertTeamLogoMutable(Team team) {
        assertTeamJoinable(team);
        if (!(team.getStatus() == TeamStatus.PENDING || team.getStatus() == TeamStatus.APPROVED)) {
            throw new ConflictException("logo cannot be changed for this team status");
        }
    }

    private void assertCaptain(Team team, String userId) {
        if (!Objects.equals(team.getCaptainUserId(), userId)) {
            throw new ForbiddenException("captain only");
        }
    }

    private static boolean isAdmin(JwtPrincipal principal) {
        return principal.roles() != null && principal.roles().contains("ADMIN");
    }

    private User getActiveUser(String userId) {
        User u = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("user not found"));
        if (u.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("user is not active");
        }
        return u;
    }

    private TeamPublicResponse toPublic(Team team, TeamCompetitionSummaryResponse summaryOrNull) {
        List<String> sponsors =
                team.getSponsorLines() == null ? List.of() : List.copyOf(team.getSponsorLines());
        return new TeamPublicResponse(
                team.getId(),
                team.getName(),
                team.getTag(),
                team.getRegionServer(),
                team.getLogoUrl(),
                team.getStatus(),
                team.getMemberUserIds() == null ? 0 : team.getMemberUserIds().size(),
                team.getCreatedAt(),
                sponsors,
                team.getCanonicalStreamUrl(),
                summaryOrNull);
    }

    private TeamCaptainViewResponse toCaptainView(Team team) {
        User captain = userRepository.findById(team.getCaptainUserId()).orElseThrow();

        List<String> coachIds = team.getCoachUserIds() == null ? List.of() : team.getCoachUserIds();
        List<String> coachNames = coachIds.stream()
                .map(id -> userRepository.findById(id).orElseThrow().getUsername())
                .toList();

        List<String> memberIds = team.getMemberUserIds() == null ? List.of() : team.getMemberUserIds();
        List<String> memberNames = memberIds.stream()
                .map(id -> userRepository.findById(id).orElseThrow().getUsername())
                .toList();

        TeamCompetitionSummaryResponse comp =
                team.getStatus() == TeamStatus.APPROVED ? teamCompetitionSummaryService.summarizeApprovedTeam(team.getId())
                : null;
        List<String> sponsors =
                team.getSponsorLines() == null ? List.of() : List.copyOf(team.getSponsorLines());

        return new TeamCaptainViewResponse(
                team.getId(),
                team.getName(),
                team.getTag(),
                team.getRegionServer(),
                team.getLogoUrl(),
                team.getStatus(),
                memberIds.size(),
                team.getCreatedAt(),
                team.getCaptainUserId(),
                captain.getUsername(),
                coachIds,
                coachNames,
                memberIds,
                memberNames,
                sponsors,
                team.getCanonicalStreamUrl(),
                comp);
    }

    private static List<String> sanitizeSponsorLines(List<String> raw) {
        if (raw == null) return List.of();
        LinkedHashSet<String> uniq = new LinkedHashSet<>();
        for (String s : raw) {
            if (s == null) continue;
            String t = s.trim();
            if (!t.isEmpty()) uniq.add(t.length() > 200 ? t.substring(0, 200) : t);
            if (uniq.size() >= 15) break;
        }
        return new ArrayList<>(uniq);
    }

    private static String trimToNullCommercial(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() > 512 ? t.substring(0, 512) : (t.isEmpty() ? null : t);
    }

    private JoinRequestResponse toJoinRequest(TeamJoinRequest r, String requesterUsername) {
        return new JoinRequestResponse(
                r.getId(),
                r.getTeamId(),
                r.getRequesterUserId(),
                requesterUsername,
                r.getStatus(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

}
