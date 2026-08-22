package com.ksh.companybackend.config;

import com.ksh.companybackend.user.application.JwtTokenProvider;
import com.ksh.companybackend.user.application.JwtTokenProvider.AccessTokenClaims;
import com.ksh.companybackend.user.application.UnauthenticatedException;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

// 인증을 핸드셰이크가 아니라 CONNECT 프레임에서 한다. 브라우저의 WebSocket 생성자는
// 헤더를 붙일 수 없어서 업그레이드 요청에 토큰을 실을 방법이 아예 없다.
// 그래서 SecurityConfig 가 /api/ws 를 permitAll 로 열어두고 여기서 막는다.
@Component
public class StompAuthenticationInterceptor implements ChannelInterceptor {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public StompAuthenticationInterceptor(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        accessor.setUser(authenticate(accessor.getFirstNativeHeader(HEADER)));

        return message;
    }

    private Authentication authenticate(String header) {
        if (header == null || !header.startsWith(PREFIX)) {
            throw new UnauthenticatedException();
        }

        AccessTokenClaims claims = tokenProvider.parseAccessToken(header.substring(PREFIX.length()));

        return UsernamePasswordAuthenticationToken.authenticated(claims.userId(), null, List.of());
    }
}
