package com.leonbon.config;

import com.leonbon.users.User;
import com.leonbon.users.UserRepository;
import com.leonbon.users.UserRole;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminUserBootstrap implements ApplicationRunner {
    private final UserRepository userRepository;
    private final String adminUsernamesCsv;

    public AdminUserBootstrap(UserRepository userRepository, @Value("${app.bootstrap.adminUsernamesCsv}") String adminUsernamesCsv) {
        this.userRepository = userRepository;
        this.adminUsernamesCsv = adminUsernamesCsv;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> admins = Arrays.stream(adminUsernamesCsv.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .toList();

        if (admins.isEmpty()) {
            return;
        }

        for (String username : admins) {
            userRepository.findByUsername(username).ifPresent(user -> promoteIfNeeded(user));
        }
    }

    private void promoteIfNeeded(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        user.setRole(UserRole.ADMIN);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }
}
