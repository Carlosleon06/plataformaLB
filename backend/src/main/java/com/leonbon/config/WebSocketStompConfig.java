package com.leonbon.config;

import com.leonbon.auth.StompJwtChannelInterceptor;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketStompConfig implements WebSocketMessageBrokerConfigurer {

    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;

    /** Mismos origins que REST (CSV). */

    private final List<String> allowedOriginPatterns;

    public WebSocketStompConfig(
            StompJwtChannelInterceptor stompJwtChannelInterceptor,
            @Value("${app.cors.allowedOriginsCsv}") String allowedOriginsCsv
    ) {
        this.stompJwtChannelInterceptor = stompJwtChannelInterceptor;
        this.allowedOriginPatterns =
                Arrays.stream(allowedOriginsCsv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompJwtChannelInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        var endpoint = registry.addEndpoint("/ws");
        String[] origins = allowedOriginPatterns.toArray(new String[0]);
        if (origins.length == 0) {
            endpoint.setAllowedOriginPatterns("*");
        } else {
            endpoint.setAllowedOriginPatterns(origins);
        }
        endpoint.withSockJS();
    }
}
