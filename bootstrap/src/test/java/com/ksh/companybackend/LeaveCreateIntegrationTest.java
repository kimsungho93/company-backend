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
class LeaveCreateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private User park;
    private String token;

    @BeforeEach
    void setUp() {
        park = User.create("park@ibslab.com", passwordEncoder.encode("password1234"), "박철수");
        park.approve();
        users.save(park);

        token = tokenProvider.createAccessToken(park.getId(), park.getEmail());
    }

    private ResultActions submit(String body) throws Exception {
        return mockMvc.perform(post("/api/leaves")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    @DisplayName("연차를 내면 201 과 만들어진 객체를 돌려준다")
    void createsAnnualLeave() throws Exception {
        submit("""
                {"kind":"ANNUAL","startDate":"2026-08-18","endDate":"2026-08-21"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(park.getId()))
                .andExpect(jsonPath("$.name").value("박철수"))
                .andExpect(jsonPath("$.kind").value("ANNUAL"))
                .andExpect(jsonPath("$.startDate").value("2026-08-18"))
                .andExpect(jsonPath("$.endDate").value("2026-08-21"));
    }

    @Test
    @DisplayName("본문의 userId 는 무시하고 토큰의 주인으로 만든다")
    void ignoresUserIdInBody() throws Exception {
        submit("""
                {"userId":9999,"kind":"ANNUAL","startDate":"2026-08-18","endDate":"2026-08-18"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(park.getId()));
    }

    @Test
    @DisplayName("모르는 kind 면 400 INVALID_INPUT")
    void rejectsUnknownKind() throws Exception {
        submit("""
                {"kind":"SABBATICAL","startDate":"2026-08-18","endDate":"2026-08-18"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("종료일이 시작일보다 이르면 400 INVALID_DATE_RANGE")
    void rejectsReversedRange() throws Exception {
        submit("""
                {"kind":"ANNUAL","startDate":"2026-08-21","endDate":"2026-08-18"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    @DisplayName("반차를 여러 날로 내면 400 HALF_DAY_MUST_BE_SINGLE_DAY")
    void rejectsMultiDayHalfDay() throws Exception {
        submit("""
                {"kind":"HALF_DAY_AM","startDate":"2026-08-18","endDate":"2026-08-19"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HALF_DAY_MUST_BE_SINGLE_DAY"));
    }

    @Test
    @DisplayName("367일이면 400 LEAVE_RANGE_TOO_LONG")
    void rejectsTooLongRange() throws Exception {
        submit("""
                {"kind":"ANNUAL","startDate":"2026-01-01","endDate":"2027-01-02"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LEAVE_RANGE_TOO_LONG"));
    }

    @Test
    @DisplayName("토큰 없이 내면 401")
    void rejectsAnonymous() throws Exception {
        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"ANNUAL","startDate":"2026-08-18","endDate":"2026-08-18"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
