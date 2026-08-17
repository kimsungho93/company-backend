package com.ksh.companybackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RefreshTokenReuseIntegrationTest {

    private static final String EMAIL = "reuse@ibslab.com";
    private static final String PASSWORD = "password1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbc;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = User.create(EMAIL, passwordEncoder.encode(PASSWORD), "테스트");
        user.approve();
        users.save(user);

        userId = users.findByEmail(EMAIL).orElseThrow().getId();
    }

    @AfterEach
    void cleanUp() {
        jdbc.execute("DELETE FROM refresh_tokens");
        jdbc.execute("DELETE FROM users");
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

    private Cookie reissue(Cookie cookie) throws Exception {
        return mockMvc.perform(post("/api/auth/reissue").cookie(cookie))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("refreshToken");
    }

    private int aliveTokenCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
                Integer.class, userId);
    }

    @Test
    @DisplayName("이미 폐기된 토큰을 다시 쓰면 401 INVALID_TOKEN")
    void rejectsAlreadyRevokedToken() throws Exception {
        Cookie stolen = login();
        reissue(stolen);

        mockMvc.perform(post("/api/auth/reissue").cookie(stolen))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("재사용을 감지하면 그 사용자의 토큰이 전부 폐기된다 - 롤백되지 않는다")
    void reuseRevokesEveryTokenOfTheUser() throws Exception {
        Cookie stolen = login();
        login();
        reissue(stolen);

        assertThat(aliveTokenCount()).isEqualTo(2);

        mockMvc.perform(post("/api/auth/reissue").cookie(stolen))
                .andExpect(status().isUnauthorized());

        assertThat(aliveTokenCount()).isZero();
    }
}
