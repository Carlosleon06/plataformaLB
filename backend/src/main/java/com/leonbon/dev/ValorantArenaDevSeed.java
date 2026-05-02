package com.leonbon.dev;

import com.leonbon.auth.AuthService;
import com.leonbon.auth.dto.RegisterRequest;
import com.leonbon.teams.Team;
import com.leonbon.teams.TeamRepository;
import com.leonbon.teams.TeamStatus;
import com.leonbon.tournaments.BracketMatchRepository;
import com.leonbon.tournaments.GameTitle;
import com.leonbon.tournaments.Tournament;
import com.leonbon.tournaments.TournamentEntry;
import com.leonbon.tournaments.TournamentEntryRepository;
import com.leonbon.tournaments.TournamentEntryStatus;
import com.leonbon.tournaments.TournamentEntryType;
import com.leonbon.tournaments.TournamentFormat;
import com.leonbon.tournaments.TournamentLifecycleStatus;
import com.leonbon.tournaments.TournamentRepository;
import com.leonbon.users.User;
import com.leonbon.users.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Dev: crea un torneo VALORANT single-elim, los tres equipos semilla con roster de 5, entradas APPROVED y
 * {@link TournamentLifecycleStatus#REGISTRATION_CLOSED} para que el admin solo pulse "Generar bracket".
 */
@Component
@Order(9_998)
@ConditionalOnProperty(prefix = "app.dev", name = "seedValorantArena", havingValue = "true")
public class ValorantArenaDevSeed implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ValorantArenaDevSeed.class);

    private static final String SEED_PASSWORD = "bracketSeed1!";

    /** Mismos equipos que {@link FriendshipBracketDevSeed} — idempotente con esa seed. */
    private static final String[] TEAM_NAMES = {"Seed Bracket Uno", "Seed Bracket Dos", "Seed Bracket Tres"};
    private static final String[] TEAM_TAGS = {"SBU", "SBD", "SBT"};

    private static final String[][] TEAM_USERNAMES = {
        {"seedam_u1", "seedam_u2", "seedam_u3", "seedam_u4", "seedam_u5"},
        {"seedam_d1", "seedam_d2", "seedam_d3", "seedam_d4", "seedam_d5"},
        {"seedam_t1", "seedam_t2", "seedam_t3", "seedam_t4", "seedam_t5"},
    };

    private final AuthService authService;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final BracketMatchRepository bracketMatchRepository;
    private final String defaultLogoUrl;
    private final String tournamentName;

    public ValorantArenaDevSeed(
            AuthService authService,
            UserRepository userRepository,
            TeamRepository teamRepository,
            TournamentRepository tournamentRepository,
            TournamentEntryRepository tournamentEntryRepository,
            BracketMatchRepository bracketMatchRepository,
            @Value("${app.teams.defaultLogoUrl}") String defaultLogoUrl,
            @Value("${app.dev.seedValorantArenaName}") String tournamentName
    ) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.tournamentRepository = tournamentRepository;
        this.tournamentEntryRepository = tournamentEntryRepository;
        this.bracketMatchRepository = bracketMatchRepository;
        this.defaultLogoUrl = defaultLogoUrl;
        this.tournamentName = tournamentName.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        Tournament tournament =
                tournamentRepository.findFirstByNameIgnoreCaseOrderByCreatedAtDesc(tournamentName).orElse(null);
        if (tournament == null) {
            tournament = createValorantTournament();
            log.info("[valorant arena seed] Created tournament '{}'", tournament.getName());
        } else {
            if (tournament.getGame() != GameTitle.VALORANT) {
                log.warn(
                        "[valorant arena seed] Tournament '{}' exists but game is {}. Skipping.",
                        tournament.getName(),
                        tournament.getGame());
                return;
            }
            log.info("[valorant arena seed] Using existing tournament '{}'", tournament.getName());
        }

        if (tournament.getBracketSize() != null && tournament.getBracketSize() > 0) {
            log.warn(
                    "[valorant arena seed] Tournament '{}' already has bracketSize={}. Not modifying lifecycle or entries.",
                    tournament.getName(),
                    tournament.getBracketSize());
            return;
        }

        if (bracketMatchRepository.countByTournamentId(tournament.getId()) > 0) {
            log.warn("[valorant arena seed] Tournament '{}' already has matches. Skipping.", tournament.getName());
            return;
        }

        String tid = tournament.getId();
        for (int i = 0; i < TEAM_NAMES.length; i++) {
            try {
                seedOneTeam(tournament, i);
            } catch (Exception e) {
                log.error("[valorant arena seed] Team index {}: {}", i, e.getMessage(), e);
            }
        }

        long approved = tournamentEntryRepository.countByTournamentIdAndStatus(tid, TournamentEntryStatus.APPROVED);
        if (approved >= 2
                && tournament.getLifecycleStatus() == TournamentLifecycleStatus.REGISTRATION_OPEN) {
            Instant now = Instant.now();
            tournament.setLifecycleStatus(TournamentLifecycleStatus.REGISTRATION_CLOSED);
            tournament.setUpdatedAt(now);
            tournamentRepository.save(tournament);
            log.info(
                    "[valorant arena seed] Closed registration on '{}' ({} approved entries). Ready: admin → Generar bracket.",
                    tournament.getName(),
                    approved);
        } else if (tournament.getLifecycleStatus() != TournamentLifecycleStatus.REGISTRATION_CLOSED) {
            log.warn(
                    "[valorant arena seed] Registration not closed automatically (approved={}, lifecycle={}).",
                    approved,
                    tournament.getLifecycleStatus());
        }

        log.info("[valorant arena seed] Done. Seed user password: {}", SEED_PASSWORD);
    }

    private Tournament createValorantTournament() {
        Instant now = Instant.now();
        Instant regStart = now.minus(7, ChronoUnit.DAYS);
        Instant regEnd = now.minus(1, ChronoUnit.DAYS);
        Instant compStart = now.plus(1, ChronoUnit.HOURS);
        Instant compEnd = now.plus(14, ChronoUnit.DAYS);

        Tournament t = new Tournament();
        t.setName(tournamentName);
        t.setOrganizers("Leön Bon · dev seed");
        t.setGame(GameTitle.VALORANT);
        t.setFormat(TournamentFormat.SINGLE_ELIM);
        t.setLifecycleStatus(TournamentLifecycleStatus.REGISTRATION_OPEN);
        t.setRegistrationStartAt(regStart);
        t.setRegistrationEndAt(regEnd);
        t.setCompetitionStartAt(compStart);
        t.setCompetitionEndAt(compEnd);
        t.setStreamUrl(null);
        t.setRulesHtml("<p>Inscripciones y datos de prueba (<code>SEED_VALORANT_ARENA</code>).</p>");
        t.setEligibilityNotes(null);
        t.setPrizeNotes(null);
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        return tournamentRepository.save(t);
    }

    private void seedOneTeam(Tournament tournament, int teamIndex) {
        String teamName = TEAM_NAMES[teamIndex];
        String teamTag = TEAM_TAGS[teamIndex];
        String[] usernames = TEAM_USERNAMES[teamIndex];

        List<User> members = new ArrayList<>(5);
        for (String username : usernames) {
            members.add(ensureUser(username));
        }
        User captain = members.get(0);

        Team team = teamRepository
                .findByNameIgnoreCase(teamName)
                .orElseGet(() -> createApprovedTeam(teamName, teamTag, captain, members));

        if (tournamentEntryRepository.findByTournamentIdAndTeamId(tournament.getId(), team.getId()).isPresent()) {
            log.info("[valorant arena seed] Entry already exists for team '{}' — skip.", teamName);
            return;
        }

        TournamentEntry entry = new TournamentEntry();
        entry.setTournamentId(tournament.getId());
        entry.setType(TournamentEntryType.TEAM);
        entry.setTeamId(team.getId());
        entry.setPlayerId(null);
        entry.setStatus(TournamentEntryStatus.APPROVED);
        entry.setSelectedRosterUserIds(members.stream().map(User::getId).toList());
        Instant now = Instant.now();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        tournamentEntryRepository.save(entry);
        log.info(
                "[valorant arena seed] APPROVED entry for team '{}' → '{}'", teamName, tournament.getName());
    }

    private User ensureUser(String username) {
        return userRepository
                .findByUsername(username)
                .orElseGet(() -> {
                    RegisterRequest req = new RegisterRequest();
                    req.setUsername(username);
                    req.setPassword(SEED_PASSWORD);
                    req.setNickname(username);
                    authService.register(req);
                    return userRepository.findByUsername(username).orElseThrow();
                });
    }

    private Team createApprovedTeam(String teamName, String teamTag, User captain, List<User> members) {
        if (teamRepository.existsByNameIgnoreCase(teamName)) {
            return teamRepository.findByNameIgnoreCase(teamName).orElseThrow();
        }
        Team team = new Team();
        team.setName(teamName);
        team.setTag(teamTag);
        team.setRegionServer("LAN");
        team.setLogoUrl(defaultLogoUrl);
        team.setStatus(TeamStatus.APPROVED);
        team.setCaptainUserId(captain.getId());
        team.setMemberUserIds(new ArrayList<>(members.stream().map(User::getId).toList()));
        team.setCoachUserIds(new ArrayList<>());
        Instant now = Instant.now();
        team.setCreatedAt(now);
        team.setUpdatedAt(now);
        team = teamRepository.save(team);
        log.info("[valorant arena seed] Created approved team '{}' [{}]", teamName, teamTag);
        return team;
    }
}
