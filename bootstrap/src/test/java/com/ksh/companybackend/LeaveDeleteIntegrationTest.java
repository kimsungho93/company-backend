package com.ksh.companybackend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.ksh.companybackend.user.application.JwtTokenProvider;
import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class LeaveDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String parkToken;
    private String adminToken;
    private Long parkLeaveId;

    @BeforeEach
    void setUp() throws Exception {
        parkToken = tokenFor(approvedUser("park@ibslab.com", "박철수", false));
        adminToken = tokenFor(approvedUser("admin@ibslab.com", "관리자", true));

        parkLeaveId = createLeave(parkToken, "2026-08-18", "2026-08-21");
    }

    private User approvedUser(String email, String name, boolean admin) {
        User user = User.create(email, passwordEncoder.encode("password1234"), name);
        user.approve();
        if (admin) {
            user.grantAdmin();
        }
        users.save(user);

        return user;
    }

    private String tokenFor(User user) {
        return tokenProvider.createAccessToken(user.getId(), user.getEmail());
    }

    private Long createLeave(String token, String start, String end) throws Exception {
        String body = mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"ANNUAL","startDate":"%s","endDate":"%s"}
                                """.formatted(start, end)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions remove(String token, Long id) throws Exception {
        return mockMvc.perform(delete("/api/leaves/{id}", id).header("Authorization", "Bearer " + token));
    }

    private ResultActions listAugust(String token) throws Exception {
        return mockMvc.perform(get("/api/leaves")
                .header("Authorization", "Bearer " + token)
                .param("from", "2026-08-01")
                .param("to", "2026-08-31"));
    }

    @Test
    @DisplayName("본인 휴가를 지우면 204 이고 목록에서 사라진다")
    void deletesOwnLeave() throws Exception {
        remove(parkToken, parkLeaveId).andExpect(status().isNoContent());

        listAugust(parkToken).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("남의 휴가는 지우지 못한다")
    void rejectsDeletingSomeoneElsesLeave() throws Exception {
        String kimToken = tokenFor(approvedUser("kim@ibslab.com", "김영희", false));

        remove(kimToken, parkLeaveId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        listAugust(parkToken).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("관리자여도 남의 휴가는 지우지 못한다")
    void rejectsAdminDeletingSomeoneElsesLeave() throws Exception {
        remove(adminToken, parkLeaveId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("없는 id 면 404 LEAVE_NOT_FOUND")
    void rejectsUnknownId() throws Exception {
        remove(parkToken, 9_999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LEAVE_NOT_FOUND"));
    }

    @Test
    @DisplayName("지운 자리에 같은 기간으로 다시 낼 수 있다")
    void freesTheDatesItOccupied() throws Exception {
        remove(parkToken, parkLeaveId).andExpect(status().isNoContent());

        createLeave(parkToken, "2026-08-18", "2026-08-21");
    }

    @Test
    @DisplayName("토큰 없이 지우면 401")
    void rejectsAnonymous() throws Exception {
        mockMvc.perform(delete("/api/leaves/{id}", parkLeaveId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
