package com.leonbon.dev;

import com.leonbon.auth.AuthService;
import com.leonbon.auth.dto.RegisterRequest;
import com.leonbon.teams.Team;
import com.leonbon.teams.TeamRepository;
import com.leonbon.teams.TeamStatus;
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
 * Dev-only: varios equipos APPROVED con roster de 5 jugadores (listos para inscribir en torneos
 * Valorant/Fortnite que crees desde admin). No crea torneos ni entradas.
 */
@Component
@Order(10_001)
@ConditionalOnProperty(prefix = "app.dev", name = "seedTeamsPool", havingValue = "true")
public class TeamsPoolDevSeed implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TeamsPoolDevSeed.class);

    /** Misma convencion que otras seeds dev; login con username + esta password. */
    public static final String SEED_PASSWORD = "poolSeed1!";

    private static final int ROSTER_SIZE = 5;

    private static final String[] TEAM_NAMES = {
        "Pool Rojo",
        "Pool Azul",
        "Pool Jade",
        "Pool Oro",
        "Pool Luna",
        "Pool Nova",
        "Pool Rex",
        "Pool Zed",
    };

    private static final String[] TEAM_TAGS = {"PRJ", "PAZ", "PJD", "POR", "PLN", "PNV", "PRX", "PZD"};

    /** pool01u1..pool01u5 (capitan = primero), pool02u1.., etc. */
    private static final String[][] TEAM_USERNAMES = {
        {"pool01u1", "pool01u2", "pool01u3", "pool01u4", "pool01u5"},
        {"pool02u1", "pool02u2", "pool02u3", "pool02u4", "pool02u5"},
        {"pool03u1", "pool03u2", "pool03u3", "pool03u4", "pool03u5"},
        {"pool04u1", "pool04u2", "pool04u3", "pool04u4", "pool04u5"},
        {"pool05u1", "pool05u2", "pool05u3", "pool05u4", "pool05u5"},
        {"pool06u1", "pool06u2", "pool06u3", "pool06u4", "pool06u5"},
        {"pool07u1", "pool07u2", "pool07u3", "pool07u4", "pool07u5"},
        {"pool08u1", "pool08u2", "pool08u3", "pool08u4", "pool08u5"},
    };

    private final AuthService authService;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final String defaultLogoUrl;

    public TeamsPoolDevSeed(
            AuthService authService,
            UserRepository userRepository,
            TeamRepository teamRepository,
            @Value("${app.teams.defaultLogoUrl}") String defaultLogoUrl) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.defaultLogoUrl = defaultLogoUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        int createdTeams = 0;
        int existingTeams = 0;

        for (int i = 0; i < TEAM_NAMES.length; i++) {
            try {
                boolean created = seedOneTeam(i);
                if (created) {
                    createdTeams++;
                } else {
                    existingTeams++;
                }
            } catch (Exception e) {
                log.error("[teams pool seed] Team index {} ({}): {}", i, TEAM_NAMES[i], e.getMessage(), e);
            }
        }

        log.info(
                "[teams pool seed] Done. {} equipos nuevos, {} ya existian ({} equipos x {} jugadores). Password: {}",
                createdTeams,
                existingTeams,
                TEAM_NAMES.length,
                ROSTER_SIZE,
                SEED_PASSWORD);
    }

    /** @return true si se creo el equipo en esta corrida */
    private boolean seedOneTeam(int teamIndex) {
        String teamName = TEAM_NAMES[teamIndex];
        String teamTag = TEAM_TAGS[teamIndex];
        String[] usernames = TEAM_USERNAMES[teamIndex];

        if (teamRepository.findByNameIgnoreCase(teamName).isPresent()) {
            log.info("[teams pool seed] Equipo '{}' ya existe — skip.", teamName);
            return false;
        }

        List<User> members = new ArrayList<>(ROSTER_SIZE);
        for (String username : usernames) {
            members.add(ensureUser(username));
        }
        User captain = members.get(0);
        createApprovedTeam(teamName, teamTag, captain, members);
        log.info("[teams pool seed] Creado '{}' [{}] con {} miembros.", teamName, teamTag, members.size());
        return true;
    }

    private User ensureUser(String username) {
        return userRepository
                .findByUsername(username)
                .orElseGet(() -> {
                    RegisterRequest req = new RegisterRequest();
                    req.setUsername(username);
                    req.setEmail(username + "@seed.pool");
                    req.setPassword(SEED_PASSWORD);
                    req.setNickname(username);
                    authService.register(req);
                    return userRepository.findByUsername(username).orElseThrow();
                });
    }

    private void createApprovedTeam(String teamName, String teamTag, User captain, List<User> members) {
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
        teamRepository.save(team);
    }
}
