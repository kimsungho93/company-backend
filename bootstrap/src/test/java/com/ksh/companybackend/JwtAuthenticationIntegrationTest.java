package com.ksh.companybackend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ksh.companybackend.user.application.JwtTokenProvider;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class JwtAuthenticationIntegrationTest {

    // anyRequest().authenticated() 라 매핑되지 않은 경로도 필터와 엔트리포인트를 그대로 탄다.
    // 인증을 통과하면 핸들러가 없어 404 가 되는데, 그 자체가 인증 성공의 신호다.
    private static final String PROTECTED = "/api/probe";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Value("${app.jwt.secret}")
    private String secret;

    @Test
    @DisplayName("유효한 토큰이면 인증을 통과한다 - 404 는 핸들러가 없다는 뜻이다")
    void acceptsValidToken() throws Exception {
        String token = tokenProvider.createAccessToken(42L, "tiger@ibslab.com");

        mockMvc.perform(get(PROTECTED).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("토큰 없이 보호 경로를 부르면 401 UNAUTHENTICATED")
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get(PROTECTED))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("만료된 토큰이면 401 TOKEN_EXPIRED")
    void rejectsExpiredToken() throws Exception {
        String expired = new JwtTokenProvider(secret, Duration.ofMinutes(-1))
                .createAccessToken(42L, "tiger@ibslab.com");

        mockMvc.perform(get(PROTECTED).header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.message").value("인증이 만료되었습니다."));
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰이면 401 INVALID_TOKEN")
    void rejectsForgedToken() throws Exception {
        String forged = new JwtTokenProvider("attacker-secret-value-at-least-32-bytes!", Duration.ofMinutes(30))
                .createAccessToken(42L, "tiger@ibslab.com");

        mockMvc.perform(get(PROTECTED).header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 인증입니다."));
    }

    @Test
    @DisplayName("JWT 형식이 아니면 401 INVALID_TOKEN")
    void rejectsGarbageToken() throws Exception {
        mockMvc.perform(get(PROTECTED).header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("쓰레기 토큰을 들고 와도 permitAll 인 로그인은 동작한다")
    void permitAllPathIgnoresBrokenToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", "Bearer not-a-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@ibslab.com","password":"password1234"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
