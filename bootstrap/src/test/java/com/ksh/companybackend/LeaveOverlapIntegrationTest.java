package com.ksh.companybackend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class LeaveOverlapIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String parkToken;
    private String kimToken;

    @BeforeEach
    void setUp() {
        parkToken = tokenFor("park@ibslab.com", "박철수");
        kimToken = tokenFor("kim@ibslab.com", "김영희");
    }

    private String tokenFor(String email, String name) {
        User user = User.create(email, passwordEncoder.encode("password1234"), name);
        user.approve();
        users.save(user);

        return tokenProvider.createAccessToken(user.getId(), user.getEmail());
    }

    private ResultActions submit(String token, String kind, String start, String end) throws Exception {
        return mockMvc.perform(post("/api/leaves")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"kind":"%s","startDate":"%s","endDate":"%s"}
                        """.formatted(kind, start, end)));
    }

    @Test
    @DisplayName("같은 사람이 겹치는 휴가를 내면 409 이고 무엇과 겹치는지 알려준다")
    void rejectsOverlappingLeave() throws Exception {
        submit(parkToken, "ANNUAL", "2026-08-18", "2026-08-21").andExpect(status().isCreated());

        submit(parkToken, "ANNUAL", "2026-08-20", "2026-08-22")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LEAVE_OVERLAP"))
                .andExpect(jsonPath("$.message")
                        .value("8월 18일부터 연차가 이미 있습니다. 기존 휴가를 지우고 다시 등록해 주세요."));
    }

    @Test
    @DisplayName("하루만 닿아도 겹침이다")
    void rejectsTouchingByOneDay() throws Exception {
        submit(parkToken, "ANNUAL", "2026-08-18", "2026-08-21").andExpect(status().isCreated());

        submit(parkToken, "ANNUAL", "2026-08-21", "2026-08-22").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("하루 벌어지면 낼 수 있다")
    void allowsAdjacentDay() throws Exception {
        submit(parkToken, "ANNUAL", "2026-08-18", "2026-08-21").andExpect(status().isCreated());

        submit(parkToken, "ANNUAL", "2026-08-22", "2026-08-23").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("연차 기간 안에 반차를 겹쳐 낼 수 없다")
    void rejectsHalfDayInsideAnnual() throws Exception {
        submit(parkToken, "ANNUAL", "2026-08-18", "2026-08-21").andExpect(status().isCreated());

        submit(parkToken, "HALF_DAY_AM", "2026-08-19", "2026-08-19")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LEAVE_OVERLAP"));
    }

    @Test
    @DisplayName("오전·오후 반차를 나눠 내는 것도 막는다 - 하루를 통째로 쉬면 연차다")
    void rejectsAmAndPmOnSameDay() throws Exception {
        submit(parkToken, "HALF_DAY_AM", "2026-08-18", "2026-08-18").andExpect(status().isCreated());

        submit(parkToken, "HALF_DAY_PM", "2026-08-18", "2026-08-18")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("8월 18일부터 오전반차가 이미 있습니다. 기존 휴가를 지우고 다시 등록해 주세요."));
    }

    @Test
    @DisplayName("다른 사람은 같은 날 내도 된다")
    void allowsOtherUserOnSameDates() throws Exception {
        submit(parkToken, "ANNUAL", "2026-08-18", "2026-08-21").andExpect(status().isCreated());

        submit(kimToken, "ANNUAL", "2026-08-18", "2026-08-21").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("요청 자체가 틀렸으면 겹침보다 그것을 먼저 알려준다")
    void reportsOwnInvalidityBeforeOverlap() throws Exception {
        submit(parkToken, "ANNUAL", "2026-08-18", "2026-08-21").andExpect(status().isCreated());

        submit(parkToken, "HALF_DAY_AM", "2026-08-19", "2026-08-21")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HALF_DAY_MUST_BE_SINGLE_DAY"));
    }
}
