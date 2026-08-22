package com.ksh.companybackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ksh.companybackend.user.application.JwtTokenProvider;
import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
class LeaveListIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @PersistenceContext
    private EntityManager entityManager;

    private User park;
    private User kim;
    private String parkToken;

    @BeforeEach
    void setUp() throws Exception {
        park = approvedUser("park@ibslab.com", "박철수");
        kim = approvedUser("kim@ibslab.com", "김영희");
        parkToken = tokenProvider.createAccessToken(park.getId(), park.getEmail());

        submit(parkToken, "ANNUAL", "2026-08-18", "2026-08-21");
        submit(tokenProvider.createAccessToken(kim.getId(), kim.getEmail()), "HALF_DAY_AM", "2026-08-25", "2026-08-25");
    }

    private User approvedUser(String email, String name) {
        User user = User.create(email, passwordEncoder.encode("password1234"), name);
        user.approve();
        users.save(user);

        return user;
    }

    private void submit(String token, String kind, String start, String end) throws Exception {
        mockMvc.perform(post("/api/leaves")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"%s","startDate":"%s","endDate":"%s"}
                                """.formatted(kind, start, end)))
                .andExpect(status().isCreated());
    }

    private ResultActions list(String from, String to) throws Exception {
        return mockMvc.perform(get("/api/leaves")
                .header("Authorization", "Bearer " + parkToken)
                .param("from", from)
                .param("to", to));
    }

    @Test
    @DisplayName("본인 것만이 아니라 전 직원의 휴가를 돌려준다")
    void listsEveryonesLeaves() throws Exception {
        list("2026-08-01", "2026-08-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$..name", containsInAnyOrder("박철수", "김영희")));
    }

    @Test
    @DisplayName("긴 휴가는 중간 날짜만 물어도 나오고 기간이 잘리지 않는다")
    void includesLeaveSpanningTheWindow() throws Exception {
        list("2026-08-19", "2026-08-19")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].userId").value(park.getId()))
                .andExpect(jsonPath("$[0].name").value("박철수"))
                .andExpect(jsonPath("$[0].kind").value("ANNUAL"))
                .andExpect(jsonPath("$[0].startDate").value("2026-08-18"))
                .andExpect(jsonPath("$[0].endDate").value("2026-08-21"));
    }

    @Test
    @DisplayName("창에 하루라도 닿으면 나오고 벗어나면 나오지 않는다")
    void includesOnlyLeavesTouchingTheWindow() throws Exception {
        list("2026-08-21", "2026-08-24")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("박철수"));

        list("2026-08-22", "2026-08-24").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("종료일이 시작일보다 이르면 400 INVALID_DATE_RANGE")
    void rejectsReversedWindow() throws Exception {
        list("2026-08-31", "2026-08-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    @DisplayName("from 이 없으면 400 이고 화면에 띄울 수 있는 문구를 준다")
    void rejectsMissingFrom() throws Exception {
        mockMvc.perform(get("/api/leaves")
                        .header("Authorization", "Bearer " + parkToken)
                        .param("to", "2026-08-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("조회 시작일을 입력해 주세요."));
    }

    @Test
    @DisplayName("토큰 없이 조회하면 401")
    void rejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/leaves").param("from", "2026-08-01").param("to", "2026-08-31"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("사람이 몇이든 쿼리는 둘 - 휴가 목록 하나, 이름 하나")
    void doesNotQueryPerPerson() throws Exception {
        User lee = approvedUser("lee@ibslab.com", "이민수");
        User choi = approvedUser("choi@ibslab.com", "최지우");
        submit(tokenProvider.createAccessToken(lee.getId(), lee.getEmail()), "ANNUAL", "2026-08-10", "2026-08-10");
        submit(tokenProvider.createAccessToken(choi.getId(), choi.getEmail()), "ANNUAL", "2026-08-11", "2026-08-11");

        // 1차 캐시가 살아 있으면 사용자 조회가 SQL 없이 끝나 N+1 이 관측되지 않는다.
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        list("2026-08-01", "2026-08-31").andExpect(jsonPath("$.length()").value(4));

        assertThat(statistics.getPrepareStatementCount())
                .describedAs("네 사람의 휴가를 읽는 데 쓴 쿼리 수")
                .isEqualTo(2);
    }
}
