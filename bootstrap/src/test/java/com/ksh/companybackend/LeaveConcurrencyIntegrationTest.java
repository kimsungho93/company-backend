package com.ksh.companybackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.ksh.companybackend.user.application.JwtTokenProvider;
import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
class LeaveConcurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JdbcTemplate jdbc;

    private Long userId;
    private String token;

    @BeforeEach
    void setUp() {
        User park = User.create("race@ibslab.com", passwordEncoder.encode("password1234"), "박철수");
        park.approve();
        users.save(park);

        userId = users.findByEmail("race@ibslab.com").orElseThrow().getId();
        token = tokenProvider.createAccessToken(userId, "race@ibslab.com");
    }

    @AfterEach
    void cleanUp() {
        jdbc.execute("DELETE FROM leaves");
        jdbc.execute("DELETE FROM users");
    }

    private Callable<Integer> submitAt(CountDownLatch gate, String start, String end) {
        return () -> {
            gate.await();
            return mockMvc.perform(post("/api/leaves")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"kind":"ANNUAL","startDate":"%s","endDate":"%s"}
                                    """.formatted(start, end)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };
    }

    @Test
    @DisplayName("더블클릭처럼 같은 휴가를 동시에 두 번 내면 하나만 저장된다")
    void doubleSubmitCreatesOnlyOne() throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<Integer> first = pool.submit(submitAt(gate, "2026-08-18", "2026-08-21"));
        Future<Integer> second = pool.submit(submitAt(gate, "2026-08-18", "2026-08-21"));
        gate.countDown();

        List<Integer> statuses = List.of(first.get(), second.get());
        pool.shutdown();

        Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM leaves WHERE user_id = ?", Integer.class, userId);

        assertThat(rows).describedAs("저장된 휴가 수 (응답: %s)", statuses).isEqualTo(1);
        assertThat(statuses).describedAs("하나는 201, 하나는 409 여야 한다").containsExactlyInAnyOrder(201, 409);
    }

    @Test
    @DisplayName("겹치지 않으면 동시에 내도 둘 다 저장된다")
    void concurrentNonOverlappingRequestsBothSucceed() throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<Integer> first = pool.submit(submitAt(gate, "2026-08-18", "2026-08-19"));
        Future<Integer> second = pool.submit(submitAt(gate, "2026-09-10", "2026-09-11"));
        gate.countDown();

        List<Integer> statuses = List.of(first.get(), second.get());
        pool.shutdown();

        Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM leaves WHERE user_id = ?", Integer.class, userId);

        assertThat(statuses).describedAs("둘 다 성공해야 한다").containsExactly(201, 201);
        assertThat(rows).isEqualTo(2);
    }
}
