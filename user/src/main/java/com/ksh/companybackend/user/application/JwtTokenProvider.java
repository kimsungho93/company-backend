package com.ksh.companybackend.user.application;

import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.access-token-ttl}") Duration accessTokenTtl) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 은 256비트 이상을 요구한다. 여기서 안 막으면 첫 로그인 시점에 터진다.
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret 은 32바이트 이상이어야 한다 (현재 " + bytes.length + ")");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
        this.accessTokenTtl = accessTokenTtl;
    }

    public String createAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }
}
