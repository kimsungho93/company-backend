package com.ksh.companybackend.config;

import com.ksh.companybackend.user.application.JwtTokenProvider;
import com.ksh.companybackend.user.application.JwtTokenProvider.AccessTokenClaims;
import com.ksh.companybackend.user.application.InvalidTokenException;
import com.ksh.companybackend.user.application.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String AUTH_ERROR_ATTRIBUTE = "jwtAuthError";
    static final String ERROR_TOKEN_EXPIRED = "TOKEN_EXPIRED";
    static final String ERROR_INVALID_TOKEN = "INVALID_TOKEN";

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            // 실패해도 던지지 않는다. 던지면 permitAll 경로까지 죽어서 재로그인이 막힌다.
            // 인증 여부 판정은 인가 단계에 맡기고, 사유만 남겨 엔트리포인트가 읽게 한다.
            try {
                AccessTokenClaims claims = tokenProvider.parseAccessToken(header.substring(PREFIX.length()));
                SecurityContextHolder.getContext().setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(claims.userId(), null, List.of()));
            } catch (TokenExpiredException e) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, ERROR_TOKEN_EXPIRED);
            } catch (InvalidTokenException e) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, ERROR_INVALID_TOKEN);
            }
        }
        chain.doFilter(request, response);
    }
}
