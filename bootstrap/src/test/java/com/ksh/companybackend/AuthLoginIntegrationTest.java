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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthLoginIntegrationTest {

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

    private static final String PENDING_EMAIL = "pending@ibslab.com";
    private static final String REJECTED_EMAIL = "rejected@ibslab.com";

    @BeforeEach
    void setUp() {
        User approved = newUser(EMAIL);
        approved.approve();
        users.save(approved);

        users.save(newUser(PENDING_EMAIL));

        User rejected = newUser(REJECTED_EMAIL);
        rejected.reject();
        users.save(rejected);
    }

    private User newUser(String email) {
        return User.create(email, passwordEncoder.encode(PASSWORD), "테스트");
    }

    private String body(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    @Test
    @DisplayName("올바른 자격 증명이면 액세스 토큰과 만료 시간을 돌려준다")
    void loginSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(1800));
    }

    @Test
    @DisplayName("로그인하면 refresh 토큰을 httpOnly 쿠키로 내려준다")
    void loginSetsRefreshCookie() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("refreshToken");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
    }

    @Test
    @DisplayName("쿠키로 나간 원문은 DB 에 해시로만 남는다")
    void storesOnlyTheHashOfTheIssuedToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, PASSWORD)))
                .andReturn();

        String rawValue = result.getResponse().getCookie("refreshToken").getValue();

        assertThat(refreshTokens.findByTokenHash(RefreshToken.hash(rawValue))).isPresent();
        assertThat(refreshTokens.findByTokenHash(rawValue)).isEmpty();
    }

    @Test
    @DisplayName("로그인은 대문자 이메일도 받는다 - 가입과 달리 관대하게 처리한다")
    void loginAcceptsUppercaseEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("TEST@IBSLAB.COM", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("승인 대기 중이면 403 APPROVAL_PENDING")
    void rejectsPendingUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PENDING_EMAIL, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("APPROVAL_PENDING"));
    }

    @Test
    @DisplayName("가입이 거절됐으면 403 SIGNUP_REJECTED")
    void rejectsRejectedUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(REJECTED_EMAIL, PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SIGNUP_REJECTED"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 상태를 알려주지 않는다 - 검사 순서가 뒤집히면 깨진다")
    void doesNotRevealStatusUntilPasswordMatches() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PENDING_EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401 과 INVALID_CREDENTIALS")
    void loginFailsWithWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("없는 이메일도 비밀번호 오류와 같은 응답이다 - 가입 여부를 알려주지 않는다")
    void loginFailsWithUnknownEmailIdentically() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("nobody@ibslab.com", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("이메일 형식이 아니면 400 - DB 를 보기 전에 걸린다")
    void loginRejectsMalformedEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-an-email", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("비밀번호가 비어 있으면 400")
    void loginRejectsBlankPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(EMAIL, "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
