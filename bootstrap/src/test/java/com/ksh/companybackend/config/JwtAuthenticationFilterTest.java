package com.ksh.companybackend.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ksh.companybackend.user.application.JwtTokenProvider;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "filter-test-secret-value-at-least-32-byte";

    private final JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET, Duration.ofMinutes(30));
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Authentication authenticateWith(String headerValue) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (headerValue != null) {
            request.addHeader("Authorization", headerValue);
        }
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    @DisplayName("유효한 토큰이면 principal 에 userId 가 들어간다")
    void putsUserIdIntoContext() throws Exception {
        String token = tokenProvider.createAccessToken(42L, "tiger@ibslab.com");

        Authentication authentication = authenticateWith("Bearer " + token);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(42L);
    }

    @Test
    @DisplayName("헤더가 없으면 컨텍스트를 비운 채 통과한다")
    void leavesContextEmptyWithoutHeader() throws Exception {
        assertThat(authenticateWith(null)).isNull();
    }

    @Test
    @DisplayName("깨진 토큰이어도 예외를 던지지 않고 컨텍스트만 비어 있다")
    void doesNotThrowOnBrokenToken() throws Exception {
        assertThat(authenticateWith("Bearer not-a-jwt")).isNull();
    }
}
