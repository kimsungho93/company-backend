package com.ksh.companybackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ksh.companybackend.user.domain.RefreshToken;
import com.ksh.companybackend.user.domain.RefreshTokenRepository;
import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthReissueIntegrationTest {

    private static final String EMAIL = "test@ibslab.com";
    private static final String PASSWORD = "password1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long userId;

    @BeforeEach
    void setUp() {
        users.save(User.create(EMAIL, passwordEncoder.encode(PASSWORD), "테스트"));
        userId = users.findByEmail(EMAIL).orElseThrow().getId();
    }

    private Cookie login() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andReturn()
                .getResponse()
                .getCookie("refreshToken");
    }

    @Test
    @DisplayName("재발급하면 새 액세스 토큰과 새 refresh 쿠키를 준다")
    void reissueRotatesBothTokens() throws Exception {
        Cookie issued = login();

        Cookie rotated = mockMvc.perform(post("/api/auth/reissue").cookie(issued))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andReturn()
                .getResponse()
                .getCookie("refreshToken");

        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(issued.getValue());
    }

    @Test
    @DisplayName("재발급하면 방금 쓴 토큰은 폐기된다")
    void reissueRevokesTheUsedToken() throws Exception {
        Cookie issued = login();

        mockMvc.perform(post("/api/auth/reissue").cookie(issued))
                .andExpect(status().isOk());

        RefreshToken used = refreshTokens.findByTokenHash(RefreshToken.hash(issued.getValue())).orElseThrow();
        assertThat(used.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("쿠키가 없으면 401 UNAUTHENTICATED")
    void rejectsMissingCookie() throws Exception {
        mockMvc.perform(post("/api/auth/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("모르는 토큰이면 401 INVALID_TOKEN")
    void rejectsUnknownToken() throws Exception {
        mockMvc.perform(post("/api/auth/reissue").cookie(new Cookie("refreshToken", "no-such-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("만료된 토큰이면 401 TOKEN_EXPIRED")
    void rejectsExpiredToken() throws Exception {
        RefreshToken.Issued expired = RefreshToken.issue(userId, Duration.ofMinutes(-1));
        refreshTokens.save(expired.token());

        mockMvc.perform(post("/api/auth/reissue").cookie(new Cookie("refreshToken", expired.rawValue())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }
}
