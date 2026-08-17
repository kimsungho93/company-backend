package com.ksh.companybackend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ksh.companybackend.user.application.JwtTokenProvider;
import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class UserMeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private User save(String email) {
        User user = User.create(email, passwordEncoder.encode("password1234"), "김성호");
        users.save(user);
        return user;
    }

    // 로그인을 거치지 않고 토큰을 직접 만든다. PENDING 은 로그인이 막혀 있어서
    // 로그인으로는 이 상태의 토큰을 얻을 수 없다.
    private String tokenOf(User user) {
        return tokenProvider.createAccessToken(user.getId(), user.getEmail());
    }

    @Test
    @DisplayName("승인된 사용자는 자기 정보 다섯 개를 받는다")
    void returnsOwnProfile() throws Exception {
        User user = save("tiger@ibslab.com");
        user.approve();
        user.grantAdmin();

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tokenOf(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("tiger@ibslab.com"))
                .andExpect(jsonPath("$.name").value("김성호"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("거절된 사용자도 403 이 아니라 200 에 상태를 받는다 - 프론트가 이유를 보여줄 수 있어야 한다")
    void rejectedUserStillGetsProfile() throws Exception {
        User user = save("rejected@ibslab.com");
        user.approve();
        user.reject();

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tokenOf(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("승인 대기 중이어도 200")
    void pendingUserStillGetsProfile() throws Exception {
        User user = save("pending@ibslab.com");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + tokenOf(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("토큰 없이 부르면 401 UNAUTHENTICATED")
    void rejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
