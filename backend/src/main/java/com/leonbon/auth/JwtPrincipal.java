package com.leonbon.auth;

import java.security.Principal;
import java.util.List;

public record JwtPrincipal(String userId, String username, List<String> roles) implements Principal {
    @Override
    public String getName() {
        return userId;
    }
}

