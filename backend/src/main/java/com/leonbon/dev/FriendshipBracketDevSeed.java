package com.leonbon.dev;

import com.leonbon.auth.AuthService;
import com.leonbon.auth.dto.RegisterRequest;
import com.leonbon.teams.Team;
import com.leonbon.teams.TeamRepository;
import com.leonbon.teams.TeamStatus;
import com.leonbon.tournaments.GameTitle;
import com.leonbon.tournaments.Tournament;
import com.leonbon.tournaments.TournamentEntry;
import com.leonbon.tournaments.TournamentEntryRepository;
import com.leonbon.tournaments.TournamentEntryStatus;
import com.leonbon.tournaments.TournamentEntryType;
import com.leonbon.tournaments.TournamentRepository;
import com.leonbon.users.User;
import com.leonbon.users.UserRepository;
import java.time.Instant;
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
 * Dev-only convenience seed: three extra VALORANT teams (five accounts each) plus approved
 * registrations on the tournament named like "Torneo de la amistad", so you can close
 * registration and generate a bracket without clicking through the UI.
 */
@Component
@Order(10_000)
@ConditionalOnProperty(prefix = "app.dev", name = "seedFriendshipBracket", havingValue = "true")
public class FriendshipBracketDevSeed implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(FriendshipBracketDevSeed.class);

    private static final String SEED_PASSWORD = "bracketSeed1!";

    private static final String[] TEAM_NAMES = {"Seed Bracket Uno", "Seed Bracket Dos", "Seed Bracket Tres"};
    private static final String[] TEAM_TAGS = {"SBU", "SBD", "SBT"};
    /** Five usernames per team (captain = first). Lowercase, unique. */
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
    private final String defaultLogoUrl;
    private final String tournamentNameToSeed;

    public FriendshipBracketDevSeed(
            AuthService authService,
            UserRepository userRepository,
            TeamRepository teamRepository,
            TournamentRepository tournamentRepository,
            TournamentEntryRepository tournamentEntryRepository,
            @Value("${app.teams.defaultLogoUrl}") String defaultLogoUrl,
            @Value("${app.dev.seedFriendshipTournamentName}") String tournamentNameToSeed
    ) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.tournamentRepository = tournamentRepository;
        this.tournamentEntryRepository = tournamentEntryRepository;
        this.defaultLogoUrl = defaultLogoUrl;
        this.tournamentNameToSeed = tournamentNameToSeed.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        Tournament tournament = tournamentRepository
                .findFirstByNameIgnoreCaseOrderByCreatedAtDesc(tournamentNameToSeed)
                .orElse(null);
        if (tournament == null) {
            log.warn(
                    "[dev seed] No tournament named '{}' (case-insensitive). Create it first, or set SEED_FRIENDSHIP_TOURNAMENT_NAME, then re-run with SEED_FRIENDSHIP_BRACKET=true.",
                    tournamentNameToSeed);
            return;
        }
        if (tournament.getGame() != GameTitle.VALORANT) {
            log.warn("[dev seed] Tournament '{}' is not VALORANT (got {}). Skipping.", tournament.getName(), tournament.getGame());
            return;
        }
        if (tournament.getBracketSize() != null && tournament.getBracketSize() > 0) {
            log.warn("[dev seed] Tournament '{}' already has bracketSize={}. Skipping seed.", tournament.getName(), tournament.getBracketSize());
            return;
        }

        for (int i = 0; i < TEAM_NAMES.length; i++) {
            try {
                seedOneTeam(tournament, i);
            } catch (Exception e) {
                log.error("[dev seed] Failed seeding team index {}: {}", i, e.getMessage(), e);
            }
        }
        log.info(
                "[dev seed] Done. Three teams (if missing) + approved entries for '{}'. Password for seed users: {}",
                tournamentNameToSeed,
                SEED_PASSWORD);
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
            log.info("[dev seed] Entry already exists for team '{}' in tournament — skip.", teamName);
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
        log.info("[dev seed] Added APPROVED tournament entry for team '{}' → tournament '{}'", teamName, tournament.getName());
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
        log.info("[dev seed] Created approved team '{}' [{}] with {} members.", teamName, teamTag, members.size());
        return team;
    }
}
