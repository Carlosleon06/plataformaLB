package com.leonbon.users;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.users.dto.MeResponse;
import com.leonbon.users.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {
    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public MeResponse me(Authentication auth) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.userId()).orElseThrow();
        UserRole role = user.getRole() == null ? UserRole.PLAYER : user.getRole();
        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getStatus(),
                role,
                user.getLeonCoinsBalance()
        );
    }
}

