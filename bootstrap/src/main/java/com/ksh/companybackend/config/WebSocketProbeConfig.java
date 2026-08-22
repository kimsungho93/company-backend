package com.ksh.companybackend.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// Netlify 의 /api/* rewrite 가 WebSocket 업그레이드를 넘기는지 확인하려고 임시로 연 경로다.
// 인증이 없으므로 확인이 끝나면 SecurityConfig 의 permitAll 과 함께 걷어낸다.
//
// SockJS 폴백을 켜지 않는다. 켜면 업그레이드가 막혀도 폴링으로 붙어서
// 통과했는지 아닌지가 가려진다 - 그걸 알려는 것이 이 확인의 목적이다.
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketProbeConfig implements WebSocketMessageBrokerConfigurer {

    private final List<String> allowedOrigins;

    public WebSocketProbeConfig(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/ws").setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
