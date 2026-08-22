package com.ksh.companybackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
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
class RoomConcurrencyIntegrationTest {

    private static final int RUSHING = 12;

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

    private String hostToken;
    private Long roomId;

    @BeforeEach
    void setUp() throws Exception {
        hostToken = tokenFor("host@ibslab.com", "김성호");

        String body = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"몰려드는 방"}
                                """))
                .andReturn().getResponse().getContentAsString();

        roomId = ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @AfterEach
    void cleanUp() {
        jdbc.execute("DELETE FROM users");
    }

    private String tokenFor(String email, String name) {
        User user = User.create(email, passwordEncoder.encode("password1234"), name);
        user.approve();
        users.save(user);

        return tokenProvider.createAccessToken(user.getId(), user.getEmail());
    }

    private Callable<Integer> joinAt(CountDownLatch gate, String token) {
        return () -> {
            gate.await();
            return mockMvc.perform(post("/api/rooms/{id}/join", roomId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };
    }

    @Test
    @DisplayName("한꺼번에 몰려들어도 정원을 넘지 않는다")
    void neverExceedsCapacity() throws Exception {
        CountDownLatch gate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(RUSHING);

        List<Future<Integer>> results = java.util.stream.IntStream.range(0, RUSHING)
                .mapToObj(i -> pool.submit(joinAt(gate, tokenFor("rush" + i + "@ibslab.com", "참가자" + i))))
                .toList();
        gate.countDown();

        List<Integer> statuses = results.stream().map(future -> {
            try {
                return future.get();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toList();
        pool.shutdown();

        String body = mockMvc.perform(get("/api/rooms").header("Authorization", "Bearer " + hostToken))
                .andReturn().getResponse().getContentAsString();
        int playerCount = ((Number) JsonPath.read(body, "$[0].playerCount")).intValue();

        assertThat(playerCount).describedAs("방 인원 (응답: %s)", statuses).isEqualTo(10);
        assertThat(statuses).filteredOn(status -> status == 200).describedAs("방장 말고 9명만 들어간다").hasSize(9);
        assertThat(statuses).filteredOn(status -> status == 409).hasSize(RUSHING - 9);
    }
}
