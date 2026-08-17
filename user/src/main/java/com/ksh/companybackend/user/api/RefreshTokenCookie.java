package com.ksh.companybackend.user.api;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookie {

    public static final String NAME = "refreshToken";

    // 재발급과 로그아웃에서만 필요하다. 좁혀두면 나머지 API 요청에는 실리지 않는다.
    private static final String PATH = "/api/auth";

    private final boolean secure;
    private final Duration ttl;

    public RefreshTokenCookie(@Value("${app.refresh-token.cookie-secure}") boolean secure, @Value("${app.refresh-token.ttl}") Duration ttl) {
        this.secure = secure;
        this.ttl = ttl;
    }

    public String bake(String rawValue) {
        return build(rawValue, ttl);
    }

    public String expire() {
        return build("", Duration.ZERO);
    }

    private String build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .path(PATH)
                .sameSite("Lax")
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
