package com.ksh.companybackend.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// SockJS 폴백을 켜지 않는다. 켜면 업그레이드가 막혀도 폴링으로 붙어서 문제가 가려진다.
//
// 운영에서 프론트는 Netlify 프록시가 아니라 Railway 주소에 직접 붙는다 - rewrite 가
// 업그레이드 헤더를 떨구는 것이 실측으로 확인됐다. 그래서 소켓만 오리진이 갈리고
// 허용 목록이 필요하다. REST 는 프록시를 거쳐 오리진이 지워진다.
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final List<String> allowedOrigins;
    private final StompAuthenticationInterceptor authentication;
    private final RoomSubscriptionInterceptor subscription;

    public WebSocketConfig(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins,
            StompAuthenticationInterceptor authentication, RoomSubscriptionInterceptor subscription) {
        this.allowedOrigins = allowedOrigins;
        this.authentication = authentication;
        this.subscription = subscription;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/ws").setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authentication, subscription);
    }
}
