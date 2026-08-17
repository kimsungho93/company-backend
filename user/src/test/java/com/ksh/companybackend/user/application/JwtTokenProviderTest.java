package com.ksh.companybackend.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ksh.companybackend.user.application.JwtTokenProvider.AccessTokenClaims;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-value-at-least-32-bytes";

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, Duration.ofMinutes(30));

    @Test
    @DisplayName("발급한 토큰을 파싱하면 userId 와 email 이 나온다")
    void parsesTokenItIssued() {
        String token = provider.createAccessToken(42L, "tiger@ibslab.com");

        AccessTokenClaims claims = provider.parseAccessToken(token);

        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.email()).isEqualTo("tiger@ibslab.com");
    }

    @Test
    @DisplayName("만료된 토큰이면 TokenExpiredException")
    void rejectsExpiredToken() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, Duration.ofMinutes(-1));
        String token = expiredProvider.createAccessToken(42L, "tiger@ibslab.com");

        assertThatThrownBy(() -> provider.parseAccessToken(token))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰이면 InvalidTokenException")
    void rejectsTokenSignedWithOtherKey() {
        JwtTokenProvider attacker = new JwtTokenProvider("attacker-secret-value-at-least-32-bytes!", Duration.ofMinutes(30));
        String token = attacker.createAccessToken(42L, "tiger@ibslab.com");

        assertThatThrownBy(() -> provider.parseAccessToken(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("typ 가 access 가 아니면 InvalidTokenException")
    void rejectsNonAccessToken() {
        String refreshToken = Jwts.builder()
                .subject("42")
                .claim("typ", "refresh")
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .compact();

        assertThatThrownBy(() -> provider.parseAccessToken(refreshToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("JWT 형식이 아니면 InvalidTokenException")
    void rejectsGarbage() {
        assertThatThrownBy(() -> provider.parseAccessToken("not-a-jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
