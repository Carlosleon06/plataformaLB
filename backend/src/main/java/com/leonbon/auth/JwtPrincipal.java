package com.leonbon.auth;

import java.util.List;

public record JwtPrincipal(String userId, String username, List<String> roles) {}

