package com.leonbon.users;

import com.leonbon.auth.JwtPrincipal;
import com.leonbon.users.dto.MeResponse;
import com.leonbon.users.dto.PatchMyProfileRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {
    private final UserRepository userRepository;
    private final UserProfileService userProfileService;

    public MeController(UserRepository userRepository, UserProfileService userProfileService) {
        this.userRepository = userRepository;
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public MeResponse me(Authentication auth) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.userId()).orElseThrow();
        return userProfileService.toMe(user);
    }

    @PatchMapping("/me/profile")
    public MeResponse patchMe(Authentication auth, @Valid @RequestBody PatchMyProfileRequest body) {
        JwtPrincipal principal = (JwtPrincipal) auth.getPrincipal();
        User updated = userProfileService.patchProfile(principal, body);
        return userProfileService.toMe(updated);
    }
}
