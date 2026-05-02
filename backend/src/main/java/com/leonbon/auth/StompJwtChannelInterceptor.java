package com.leonbon.auth;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {
    private final JwtService jwtService;

    public StompJwtChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null || acc.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        List<String> rawHeaders = acc.getNativeHeader("Authorization");
        String authHeader = rawHeaders != null && !rawHeaders.isEmpty() ? rawHeaders.get(0) : null;
        String token = bearerToken(authHeader);
        if (token == null) {
            return message;
        }
        try {
            JwtPrincipal principal = jwtService.parse(token);
            var authorities =
                    principal.roles().stream()
                            .map(StompJwtChannelInterceptor::toAuthority)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            acc.setUser(authentication);
            acc.removeNativeHeader("Authorization");
        } catch (Exception ignored) {
            /* token inválido: la conexión sigue pero sin usuario (solo topics públicos) */
        }
        return message;
    }

    private static String bearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring("Bearer ".length()).trim();
    }

    private static String toAuthority(String role) {
        if (role == null) {
            return "ROLE_PLAYER";
        }
        return switch (role) {
            case "ADMIN" -> "ROLE_ADMIN";
            default -> "ROLE_PLAYER";
        };
    }
}
