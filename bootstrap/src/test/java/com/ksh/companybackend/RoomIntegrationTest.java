package com.ksh.companybackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.ksh.companybackend.user.application.JwtTokenProvider;
import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import java.util.List;
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
class RoomIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String hostToken;
    private String guestToken;

    @BeforeEach
    void setUp() {
        hostToken = tokenFor("host@ibslab.com", "김성호");
        guestToken = tokenFor("guest@ibslab.com", "박철수");
    }

    private String tokenFor(String email, String name) {
        User user = User.create(email, passwordEncoder.encode("password1234"), name);
        user.approve();
        users.save(user);

        return tokenProvider.createAccessToken(user.getId(), user.getEmail());
    }

    private ResultActions create(String token, String body) throws Exception {
        return mockMvc.perform(post("/api/rooms")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Long createdRoomId(String token, String name) throws Exception {
        String body = create(token, """
                {"name":"%s"}
                """.formatted(name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions join(String token, Long roomId, String body) throws Exception {
        var request = post("/api/rooms/{id}/join", roomId).header("Authorization", "Bearer " + token);
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }

        return mockMvc.perform(request);
    }

    private List<Integer> listedRoomIds() throws Exception {
        String body = mockMvc.perform(get("/api/rooms").header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(body, "$..id");
    }

    @Test
    @DisplayName("방을 만들면 201 과 방 정보를 돌려주고 방장이 이미 들어가 있다")
    void createsRoom() throws Exception {
        create(hostToken, """
                {"name":"점심내기 한판"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("점심내기 한판"))
                .andExpect(jsonPath("$.hostName").value("김성호"))
                .andExpect(jsonPath("$.playerCount").value(1))
                .andExpect(jsonPath("$.capacity").value(10))
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("비밀번호를 걸면 잠긴 방이 되고 비밀번호는 응답에 없다")
    void neverLeaksThePassword() throws Exception {
        String body = create(hostToken, """
                {"name":"비밀방","password":"1234"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.locked").value(true))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("1234").doesNotContain("password");
    }

    @Test
    @DisplayName("이름은 앞뒤 공백을 뺀 1~29자여야 한다")
    void rejectsBadName() throws Exception {
        create(hostToken, """
                {"name":"   "}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        create(hostToken, """
                {"name":"%s"}
                """.formatted("가".repeat(30)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        create(hostToken, """
                {"name":"  %s  "}
                """.formatted("가".repeat(29)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("가".repeat(29)));
    }

    @Test
    @DisplayName("목록에 최근 만든 방이 먼저 나온다")
    void listsNewestFirst() throws Exception {
        Long older = createdRoomId(hostToken, "먼저 만든 방");
        Long newer = createdRoomId(guestToken, "나중 만든 방");

        List<Integer> ids = listedRoomIds();

        assertThat(ids).contains(older.intValue(), newer.intValue());
        assertThat(ids.indexOf(newer.intValue())).isLessThan(ids.indexOf(older.intValue()));
    }

    @Test
    @DisplayName("들어가면 200 이고 인원이 는다")
    void joinsRoom() throws Exception {
        Long roomId = createdRoomId(hostToken, "점심내기 한판");

        join(guestToken, roomId, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roomId))
                .andExpect(jsonPath("$.playerCount").value(2))
                .andExpect(jsonPath("$.hostName").value("김성호"));
    }

    @Test
    @DisplayName("같은 사람이 두 번 들어가도 인원이 늘지 않는다")
    void joiningTwiceDoesNotCount() throws Exception {
        Long roomId = createdRoomId(hostToken, "점심내기 한판");

        join(guestToken, roomId, "{}").andExpect(jsonPath("$.playerCount").value(2));
        join(guestToken, roomId, "{}").andExpect(jsonPath("$.playerCount").value(2));
    }

    @Test
    @DisplayName("방장이 자기 방에 다시 들어가도 방이 사라지지 않는다")
    void hostRejoiningOwnRoomKeepsIt() throws Exception {
        Long roomId = createdRoomId(hostToken, "혼자 있는 방");

        join(hostToken, roomId, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerCount").value(1));
    }

    @Test
    @DisplayName("다른 방에 들어가면 이전 방에서 빠지고, 혼자였으면 그 방이 사라진다")
    void movingOutEmptiesTheOldRoom() throws Exception {
        Long left = createdRoomId(guestToken, "떠날 방");
        Long target = createdRoomId(hostToken, "옮겨갈 방");

        join(guestToken, target, "{}").andExpect(jsonPath("$.playerCount").value(2));

        assertThat(listedRoomIds()).doesNotContain(left.intValue());
    }

    @Test
    @DisplayName("비밀번호가 맞으면 들어가고 틀리면 403")
    void checksThePassword() throws Exception {
        String body = create(hostToken, """
                {"name":"비밀방","password":"1234"}
                """).andReturn().getResponse().getContentAsString();
        Long roomId = ((Number) JsonPath.read(body, "$.id")).longValue();

        join(guestToken, roomId, """
                {"password":"9999"}
                """)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("WRONG_ROOM_PASSWORD"));

        join(guestToken, roomId, """
                {"password":"1234"}
                """).andExpect(status().isOk());
    }

    @Test
    @DisplayName("비밀번호 없는 방에 비밀번호를 실어 보내도 통과시킨다")
    void ignoresPasswordOnOpenRoom() throws Exception {
        Long roomId = createdRoomId(hostToken, "열린 방");

        join(guestToken, roomId, """
                {"password":"부질없는 비밀번호"}
                """).andExpect(status().isOk());
    }

    @Test
    @DisplayName("본문 없이 들어가도 된다")
    void allowsEmptyBody() throws Exception {
        Long roomId = createdRoomId(hostToken, "열린 방");

        join(guestToken, roomId, null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("없는 방에 들어가면 404 ROOM_NOT_FOUND")
    void rejectsUnknownRoom() throws Exception {
        join(guestToken, 9_999_999L, "{}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    @DisplayName("11번째 사람은 409 ROOM_FULL")
    void rejectsEleventhPlayer() throws Exception {
        Long roomId = createdRoomId(hostToken, "꽉 찰 방");

        for (int i = 1; i <= 9; i++) {
            join(tokenFor("p" + i + "@ibslab.com", "참가자" + i), roomId, "{}").andExpect(status().isOk());
        }

        join(guestToken, roomId, "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_FULL"));
    }

    @Test
    @DisplayName("토큰 없이는 목록도 생성도 못 한다")
    void rejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/rooms")).andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"점심내기 한판"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("들어가기에 실패하면 원래 있던 방에서 쫓겨나지 않는다")
    void failedJoinKeepsTheCurrentRoom() throws Exception {
        Long mine = createdRoomId(guestToken, "내가 혼자 있는 방");
        String body = create(hostToken, """
                {"name":"비밀방","password":"1234"}
                """).andReturn().getResponse().getContentAsString();
        Long locked = ((Number) JsonPath.read(body, "$.id")).longValue();

        join(guestToken, 9_999_999L, "{}").andExpect(status().isNotFound());
        join(guestToken, locked, """
                {"password":"9999"}
                """).andExpect(status().isForbidden());

        assertThat(listedRoomIds()).describedAs("혼자 있던 방은 내가 빠지면 사라진다").contains(mine.intValue());
    }
}
