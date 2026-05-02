package com.leonbon.teams;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.files.LocalLogoStorageService;
import com.leonbon.teams.dto.CaptainTeamSummaryResponse;
import com.leonbon.teams.dto.CreateTeamRequest;
import com.leonbon.teams.dto.JoinRequestResponse;
import com.leonbon.teams.dto.TeamCaptainViewResponse;
import com.leonbon.teams.dto.TeamPublicResponse;
import com.leonbon.auth.ConflictException;
import com.leonbon.users.User;
import com.leonbon.users.UserRepository;
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
    private final TeamRepository teamRepository;
    private final TeamJoinRequestRepository joinRequestRepository;
    private final UserRepository userRepository;
    private final String defaultLogoUrl;
    private final LocalLogoStorageService logoStorageService;

    public TeamService(
            TeamRepository teamRepository,
            TeamJoinRequestRepository joinRequestRepository,
            UserRepository userRepository,
            @Value("${app.teams.defaultLogoUrl}") String defaultLogoUrl,
            LocalLogoStorageService logoStorageService
    ) {
        this.teamRepository = teamRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.userRepository = userRepository;
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
        team.setCreatedAt(Instant.now());
        team.setUpdatedAt(Instant.now());

        try {
            team = teamRepository.save(team);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("team name already exists");
        }

        return toPublic(team);
    }

    public List<TeamPublicResponse> listApprovedTeams() {
        return teamRepository.findTop20ByStatusOrderByCreatedAtDesc(TeamStatus.APPROVED).stream()
                .map(this::toPublic)
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

    public List<TeamPublicResponse> searchApprovedTeams(String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            throw new BadRequestException("query too short");
        }
        return teamRepository.findTop50ByStatusAndNameContainingIgnoreCaseOrderByNameAsc(TeamStatus.APPROVED, q).stream()
                .map(this::toPublic)
                .toList();
    }

    public TeamPublicResponse getPublicTeam(String teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        if (team.getStatus() != TeamStatus.APPROVED) {
            throw new NotFoundException("team not found");
        }
        return toPublic(team);
    }

    public Object getTeamForViewer(JwtPrincipal principal, String teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        if (team.getStatus() == TeamStatus.APPROVED) {
            boolean isMember = team.getMemberUserIds() != null && team.getMemberUserIds().contains(principal.userId());
            if (isMember) {
                return toCaptainView(team);
            }
            return toPublic(team);
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

    public void approveTeamAsAdmin(String teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        if (team.getStatus() != TeamStatus.PENDING) {
            throw new ConflictException("team is not pending");
        }
        team.setStatus(TeamStatus.APPROVED);
        team.setUpdatedAt(Instant.now());
        teamRepository.save(team);
    }

    public void rejectTeamAsAdmin(String teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new NotFoundException("team not found"));
        if (team.getStatus() != TeamStatus.PENDING) {
            throw new ConflictException("team is not pending");
        }
        team.setStatus(TeamStatus.REJECTED);
        team.setUpdatedAt(Instant.now());
        teamRepository.save(team);
    }

    public void suspendTeamAsAdmin(String teamId) {
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

    public void resetLogoAdmin(String teamId) {
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

    private User getActiveUser(String userId) {
        User u = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("user not found"));
        if (u.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("user is not active");
        }
        return u;
    }

    private TeamPublicResponse toPublic(Team team) {
        return new TeamPublicResponse(
                team.getId(),
                team.getName(),
                team.getTag(),
                team.getRegionServer(),
                team.getLogoUrl(),
                team.getStatus(),
                team.getMemberUserIds() == null ? 0 : team.getMemberUserIds().size(),
                team.getCreatedAt()
        );
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
                memberNames
        );
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
