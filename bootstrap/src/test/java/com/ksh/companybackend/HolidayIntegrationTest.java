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
class HolidayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminToken = tokenFor("admin@ibslab.com", "관리자", true);
        userToken = tokenFor("park@ibslab.com", "박철수", false);
    }

    private String tokenFor(String email, String name, boolean admin) {
        User user = User.create(email, passwordEncoder.encode("password1234"), name);
        user.approve();
        if (admin) {
            user.grantAdmin();
        }
        users.save(user);

        return tokenProvider.createAccessToken(user.getId(), user.getEmail());
    }

    private ResultActions create(String token, String name, String start, String end) throws Exception {
        return mockMvc.perform(post("/api/holidays")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s","startDate":"%s","endDate":"%s"}
                        """.formatted(name, start, end)));
    }

    private Long createdId(String name, String start, String end) throws Exception {
        String body = create(adminToken, name, start, end)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions list(String token, String from, String to) throws Exception {
        return mockMvc.perform(get("/api/holidays")
                .header("Authorization", "Bearer " + token)
                .param("from", from)
                .param("to", to));
    }

    @Test
    @DisplayName("관리자가 연휴를 지정하면 201 과 만들어진 객체를 돌려준다")
    void adminCreatesHoliday() throws Exception {
        create(adminToken, "설날", "2027-02-06", "2027-02-08")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("설날"))
                .andExpect(jsonPath("$.startDate").value("2027-02-06"))
                .andExpect(jsonPath("$.endDate").value("2027-02-08"));
    }

    @Test
    @DisplayName("일반 사용자는 지정하지 못한다")
    void rejectsNonAdminCreate() throws Exception {
        create(userToken, "창립기념일", "2027-03-02", "2027-03-02")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("이름은 1자 이상 12자 이하여야 한다")
    void rejectsBadName() throws Exception {
        create(adminToken, "", "2027-03-02", "2027-03-02")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        create(adminToken, "가나다라마바사아자차카타파", "2027-03-02", "2027-03-02")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        create(adminToken, "가나다라마바사아자차카타", "2027-03-02", "2027-03-02")
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("종료일이 시작일보다 이르면 400 INVALID_DATE_RANGE")
    void rejectsReversedRange() throws Exception {
        create(adminToken, "설날", "2027-02-08", "2027-02-06")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    @DisplayName("겹치는 공휴일도 막지 않는다 - 해제하면 되돌아가므로")
    void allowsOverlappingHolidays() throws Exception {
        create(adminToken, "설날", "2027-02-06", "2027-02-08").andExpect(status().isCreated());

        create(adminToken, "임시휴무", "2027-02-07", "2027-02-07").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("조회는 일반 사용자도 한다 - 달력에 빨간 날을 그려야 하므로")
    void anyoneCanList() throws Exception {
        createdId("설날", "2027-02-06", "2027-02-08");

        list(userToken, "2027-02-01", "2027-02-28")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("설날"));
    }

    @Test
    @DisplayName("창에 걸치기만 해도 나오고 벗어나면 나오지 않는다")
    void listsOnlyHolidaysTouchingTheWindow() throws Exception {
        createdId("설날", "2027-02-06", "2027-02-08");

        list(userToken, "2027-02-08", "2027-02-10").andExpect(jsonPath("$.length()").value(1));
        list(userToken, "2027-02-09", "2027-02-10").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("from 이 없으면 400 INVALID_INPUT")
    void rejectsMissingFrom() throws Exception {
        mockMvc.perform(get("/api/holidays")
                        .header("Authorization", "Bearer " + userToken)
                        .param("to", "2027-02-28"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("관리자가 지우면 204 이고 목록에서 사라진다")
    void adminDeletesHoliday() throws Exception {
        Long id = createdId("설날", "2027-02-06", "2027-02-08");

        mockMvc.perform(delete("/api/holidays/{id}", id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        list(userToken, "2027-02-01", "2027-02-28").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("일반 사용자는 지우지 못하고, 없는 id 인지도 알려주지 않는다")
    void rejectsNonAdminDelete() throws Exception {
        mockMvc.perform(delete("/api/holidays/{id}", 9_999_999L).header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("관리자가 없는 id 를 지우면 404 HOLIDAY_NOT_FOUND")
    void rejectsUnknownId() throws Exception {
        mockMvc.perform(delete("/api/holidays/{id}", 9_999_999L).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOLIDAY_NOT_FOUND"));
    }

    @Test
    @DisplayName("토큰 없이는 조회도 지정도 못 한다")
    void rejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/holidays").param("from", "2027-02-01").param("to", "2027-02-28"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"설날","startDate":"2027-02-06","endDate":"2027-02-08"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
