package com.leonbon.auth;

import com.leonbon.auth.dto.LoginRequest;
import com.leonbon.auth.dto.RegisterRequest;
import com.leonbon.economy.Transaction;
import com.leonbon.economy.TransactionRepository;
import com.leonbon.economy.TransactionType;
import com.leonbon.infra.mongo.SequenceService;
import com.leonbon.users.User;
import com.leonbon.users.UserProfileService;
import com.leonbon.users.UserRepository;
import com.leonbon.users.UserRole;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SequenceService sequenceService;
    private final long welcomeBonus;
    private final List<String> adminUsernames;

    public AuthService(
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SequenceService sequenceService,
            @Value("${app.economy.welcomeBonus}") long welcomeBonus,
            @Value("${app.bootstrap.adminUsernamesCsv}") String adminUsernamesCsv
    ) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sequenceService = sequenceService;
        this.welcomeBonus = welcomeBonus;
        this.adminUsernames = Arrays.stream(adminUsernamesCsv.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public String register(RegisterRequest req) {
        User user = new User();
        String username = req.getUsername().trim().toLowerCase();
        user.setUsername(username);
        String normalizedEmail = UserProfileService.normalizeEmail(req.getEmail());
        if (userRepository.existsByEmailNormalized(normalizedEmail)) {
            throw new ConflictException("email already exists");
        }
        user.setEmailNormalized(normalizedEmail);

        user.setNickname(req.getNickname());
        user.setFullName(trimOrNull(req.getFullName()));
        user.setCountry(trimOrNull(req.getCountry()));
        user.setProfileShowFullName(Boolean.TRUE.equals(req.getProfileShowFullName()));
        user.setLeonPlayerNumber(sequenceService.nextUserLeonPlayerNumber());

        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(adminUsernames.contains(username) ? UserRole.ADMIN : UserRole.PLAYER);
        user.setLeonCoinsBalance(welcomeBonus);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        try {
            user = userRepository.save(user);
        } catch (DuplicateKeyException e) {
            throw new ConflictException("username or email already exists");
        }

        Transaction t = new Transaction();
        t.setUserId(user.getId());
        t.setType(TransactionType.WELCOME_BONUS);
        t.setAmount(welcomeBonus);
        t.setBalanceAfter(welcomeBonus);
        t.setCreatedAt(Instant.now());
        transactionRepository.save(t);

        return jwtService.issueToken(user.getId(), user.getUsername(), rolesFor(user));
    }

    public String login(LoginRequest req) {
        String username = req.getUsername().trim().toLowerCase();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UnauthorizedException("invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("invalid credentials");
        }
        return jwtService.issueToken(user.getId(), user.getUsername(), rolesFor(user));
    }

    private static List<String> rolesFor(User user) {
        UserRole role = user.getRole() == null ? UserRole.PLAYER : user.getRole();
        return role == UserRole.ADMIN ? List.of("ADMIN") : List.of("PLAYER");
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

