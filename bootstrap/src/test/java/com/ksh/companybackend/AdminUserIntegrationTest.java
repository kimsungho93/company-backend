package com.ksh.companybackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.ksh.companybackend.user.domain.RefreshTokenRepository;
import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import com.ksh.companybackend.user.domain.UserStatus;
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
class AdminUserIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@ibslab.com";
    private static final String MEMBER_EMAIL = "member@ibslab.com";
    private static final String PENDING_EMAIL = "pending@ibslab.com";
    private static final String PASSWORD = "password1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        User admin = newUser(ADMIN_EMAIL);
        admin.approve();
        admin.grantAdmin();
        users.save(admin);

        User member = newUser(MEMBER_EMAIL);
        member.approve();
        users.save(member);

        users.save(newUser(PENDING_EMAIL));
    }

    private User newUser(String email) {
        return User.create(email, passwordEncoder.encode(PASSWORD), "테스트");
    }

    private Long idOf(String email) {
        return users.findByEmail(email).orElseThrow().getId();
    }

    private UserStatus statusOf(String email) {
        return users.findByEmail(email).orElseThrow().getStatus();
    }

    private int aliveTokensOf(String email) {
        return refreshTokens.findAllByUserIdAndRevokedAtIsNull(idOf(email)).size();
    }

    private String accessTokenOf(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(body, "$.accessToken");
    }

    @Test
    @DisplayName("관리자가 승인하면 204 이고 상태가 APPROVED 가 된다")
    void adminApproves() throws Exception {
        mockMvc.perform(post("/api/admin/users/{id}/approve", idOf(PENDING_EMAIL))
                        .header("Authorization", "Bearer " + accessTokenOf(ADMIN_EMAIL)))
                .andExpect(status().isNoContent());

        assertThat(statusOf(PENDING_EMAIL)).isEqualTo(UserStatus.APPROVED);
    }

    @Test
    @DisplayName("관리자가 거절하면 204 이고 상태가 REJECTED 가 된다")
    void adminRejects() throws Exception {
        mockMvc.perform(post("/api/admin/users/{id}/reject", idOf(PENDING_EMAIL))
                        .header("Authorization", "Bearer " + accessTokenOf(ADMIN_EMAIL)))
                .andExpect(status().isNoContent());

        assertThat(statusOf(PENDING_EMAIL)).isEqualTo(UserStatus.REJECTED);
    }

    @Test
    @DisplayName("거절하면 그 사용자의 refresh 토큰이 전부 폐기된다 - 남은 세션이 계속 갱신되지 않는다")
    void rejectRevokesRefreshTokens() throws Exception {
        accessTokenOf(MEMBER_EMAIL);
        assertThat(aliveTokensOf(MEMBER_EMAIL)).isEqualTo(1);

        mockMvc.perform(post("/api/admin/users/{id}/reject", idOf(MEMBER_EMAIL))
                        .header("Authorization", "Bearer " + accessTokenOf(ADMIN_EMAIL)))
                .andExpect(status().isNoContent());

        assertThat(aliveTokensOf(MEMBER_EMAIL)).isZero();
    }

    @Test
    @DisplayName("일반 사용자가 부르면 403 FORBIDDEN")
    void rejectsNonAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/users/{id}/approve", idOf(PENDING_EMAIL))
                        .header("Authorization", "Bearer " + accessTokenOf(MEMBER_EMAIL)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("관리자는 상태로 걸러 사용자 목록을 본다")
    void listsUsersByStatus() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + accessTokenOf(ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(PENDING_EMAIL))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    @DisplayName("일반 사용자는 목록을 볼 수 없다")
    void listRejectsNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + accessTokenOf(MEMBER_EMAIL)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("없는 사용자를 대상으로 하면 404 USER_NOT_FOUND")
    void rejectsUnknownTarget() throws Exception {
        mockMvc.perform(post("/api/admin/users/{id}/approve", 9_999_999L)
                        .header("Authorization", "Bearer " + accessTokenOf(ADMIN_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("토큰 없이 부르면 401 UNAUTHENTICATED")
    void rejectsAnonymous() throws Exception {
        mockMvc.perform(post("/api/admin/users/{id}/approve", idOf(PENDING_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("자기 자신은 거절할 수 없다 - 유일한 관리자가 스스로를 잠그면 SQL 로만 복구된다")
    void cannotRejectSelf() throws Exception {
        mockMvc.perform(post("/api/admin/users/{id}/reject", idOf(ADMIN_EMAIL))
                        .header("Authorization", "Bearer " + accessTokenOf(ADMIN_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_REJECT_SELF"));

        assertThat(statusOf(ADMIN_EMAIL)).isEqualTo(UserStatus.APPROVED);
    }
}
