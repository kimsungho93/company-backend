package com.ksh.companybackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.ksh.companybackend.game.application.RoomService;
import com.ksh.companybackend.user.application.JwtTokenProvider;
import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@SuppressWarnings("unchecked")
class RoomSocketIntegrationTest {

    private static final int WAIT_SECONDS = 5;

    @LocalServerPort
    private int port;

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

    @Autowired
    private RoomService roomService;

    private String hostToken;
    private String guestToken;
    private String strangerToken;
    private Long roomId;

    @BeforeEach
    void setUp() throws Exception {
        hostToken = tokenFor("host@ibslab.com", "김성호");
        guestToken = tokenFor("guest@ibslab.com", "박철수");
        strangerToken = tokenFor("stranger@ibslab.com", "남남");

        roomId = createRoom();
        joinRoom(guestToken);
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

    private Long createRoom() throws Exception {
        return createRoom(hostToken, "점심내기 한판");
    }

    private Long createRoom(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void joinRoom(String token) throws Exception {
        joinRoom(token, roomId);
    }

    private void joinRoom(String token, Long targetRoomId) throws Exception {
        mockMvc.perform(post("/api/rooms/{id}/join", targetRoomId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    private WebSocketStompClient client() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new SimpleMessageConverter());

        return client;
    }

    private StompSession connect(String token) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }

        return client()
                .connectAsync("ws://localhost:%d/api/ws".formatted(port), new WebSocketHttpHeaders(),
                        connectHeaders, new StompSessionHandlerAdapter() { })
                .get(WAIT_SECONDS, TimeUnit.SECONDS);
    }

    private BlockingQueue<String> subscribe(StompSession session, String destination) {
        BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                frames.add(new String((byte[]) payload, StandardCharsets.UTF_8));
            }
        });

        return frames;
    }

    private void send(StompSession session, String destination, String json) {
        StompHeaders headers = new StompHeaders();
        headers.setDestination(destination);
        headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
        session.send(headers, json.getBytes(StandardCharsets.UTF_8));
    }

    private String next(BlockingQueue<String> frames) throws Exception {
        String frame = frames.poll(WAIT_SECONDS, TimeUnit.SECONDS);
        assertThat(frame).describedAs("브로드캐스트를 기다렸지만 오지 않았다").isNotNull();

        return frame;
    }

    @Test
    @DisplayName("토큰 없이 CONNECT 하면 연결이 거절된다")
    void rejectsConnectWithoutToken() {
        assertThatThrownBy(() -> connect(null)).hasMessageContaining("");
    }

    @Test
    @DisplayName("서명이 틀린 토큰으로 CONNECT 하면 거절된다")
    void rejectsConnectWithBadToken() {
        assertThatThrownBy(() -> connect("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.wrong")).isNotNull();
    }

    @Test
    @DisplayName("유효한 토큰이면 연결된다")
    void acceptsConnectWithValidToken() throws Exception {
        StompSession session = connect(guestToken);

        assertThat(session.isConnected()).isTrue();
        session.disconnect();
    }

    @Test
    @DisplayName("참가자가 아니면 그 방을 구독하지 못한다 - 비밀방을 엿볼 수 없다")
    void rejectsSubscribeFromNonParticipant() throws Exception {
        StompSession stranger = connect(strangerToken);
        BlockingQueue<String> peeked = subscribe(stranger, "/topic/rooms/" + roomId);

        StompSession guest = connect(guestToken);
        BlockingQueue<String> mine = subscribe(guest, "/topic/rooms/" + roomId);
        guest.send("/app/rooms/%d/enter".formatted(roomId), new byte[0]);

        next(mine);
        assertThat(peeked.poll(2, TimeUnit.SECONDS)).describedAs("남의 방 상태가 오면 안 된다").isNull();
        assertThat(stranger.isConnected()).describedAs("거절당한 세션은 끊긴다").isFalse();

        guest.disconnect();
    }

    @Test
    @DisplayName("enter 하면 방 상태 전체가 브로드캐스트된다")
    void broadcastsRoomStateOnEnter() throws Exception {
        StompSession session = connect(guestToken);
        BlockingQueue<String> frames = subscribe(session, "/topic/rooms/" + roomId);

        session.send("/app/rooms/%d/enter".formatted(roomId), new byte[0]);

        String state = next(frames);
        assertThat(((Number) JsonPath.read(state, "$.id")).longValue()).isEqualTo(roomId);
        assertThat((String) JsonPath.read(state, "$.status")).isEqualTo("WAITING");
        assertThat((List<?>) JsonPath.read(state, "$.players")).hasSize(2);
        assertThat((List<String>) JsonPath.read(state, "$.players[*].name"))
                .containsExactly("김성호", "박철수");
        session.disconnect();
    }

    @Test
    @DisplayName("leave 하면 빠지고 남은 사람에게 브로드캐스트된다")
    void broadcastsOnLeave() throws Exception {
        StompSession hostSession = connect(hostToken);
        BlockingQueue<String> hostFrames = subscribe(hostSession, "/topic/rooms/" + roomId);
        hostSession.send("/app/rooms/%d/enter".formatted(roomId), new byte[0]);
        next(hostFrames);

        StompSession guestSession = connect(guestToken);
        guestSession.send("/app/rooms/%d/leave".formatted(roomId), new byte[0]);

        String state = next(hostFrames);
        assertThat((List<?>) JsonPath.read(state, "$.players")).hasSize(1);
        assertThat((List<String>) JsonPath.read(state, "$.players[*].name")).containsExactly("김성호");

        hostSession.disconnect();
        guestSession.disconnect();
    }

    @Test
    @DisplayName("준비 상태를 보내면 브로드캐스트에 반영된다")
    void broadcastsReadyChange() throws Exception {
        StompSession guest = connect(guestToken);
        BlockingQueue<String> frames = subscribe(guest, "/topic/rooms/" + roomId);
        guest.send("/app/rooms/%d/enter".formatted(roomId), new byte[0]);
        next(frames);

        send(guest, "/app/rooms/%d/ready".formatted(roomId), "{\"ready\":true}");

        String state = next(frames);
        assertThat((List<Boolean>) JsonPath.read(state, "$.players[*].ready")).containsExactly(false, true);
        guest.disconnect();
    }

    @Test
    @DisplayName("방장이 아닌 사람이 시작하면 본인에게만 NOT_ROOM_HOST 가 온다")
    void sendsErrorToTheCallerOnly() throws Exception {
        StompSession guest = connect(guestToken);
        BlockingQueue<String> errors = subscribe(guest, "/user/queue/errors");
        BlockingQueue<String> room = subscribe(guest, "/topic/rooms/" + roomId);

        guest.send("/app/rooms/%d/start".formatted(roomId), new byte[0]);

        String error = next(errors);
        assertThat((String) JsonPath.read(error, "$.code")).isEqualTo("NOT_ROOM_HOST");
        assertThat(room.poll(1, TimeUnit.SECONDS)).describedAs("실패는 방에 브로드캐스트되지 않는다").isNull();
        guest.disconnect();
    }

    @Test
    @DisplayName("소켓이 끊기면 방에서 빠지고 남은 사람에게 알려진다")
    void removesPlayerOnDisconnect() throws Exception {
        StompSession host = connect(hostToken);
        BlockingQueue<String> frames = subscribe(host, "/topic/rooms/" + roomId);
        host.send("/app/rooms/%d/enter".formatted(roomId), new byte[0]);
        next(frames);

        StompSession guest = connect(guestToken);
        guest.send("/app/rooms/%d/enter".formatted(roomId), new byte[0]);
        next(frames);

        guest.disconnect();

        String state = next(frames);
        assertThat((List<String>) JsonPath.read(state, "$.players[*].name")).containsExactly("김성호");
        host.disconnect();
    }

    @Test
    @DisplayName("enter 하지 않고 앉아만 있던 좌석은 회수된다 - enter 한 사람은 남는다")
    void reclaimsSeatThatNeverEntered() throws Exception {
        StompSession host = connect(hostToken);
        BlockingQueue<String> frames = subscribe(host, "/topic/rooms/" + roomId);
        host.send("/app/rooms/%d/enter".formatted(roomId), new byte[0]);
        next(frames);

        roomService.reclaimSeatsAbandonedBefore(Instant.now().plusSeconds(1));

        String state = next(frames);
        assertThat((List<String>) JsonPath.read(state, "$.players[*].name")).containsExactly("김성호");
        host.disconnect();
    }

    @Test
    @DisplayName("다른 방으로 옮기면 이전 방에 남은 사람에게도 알려진다")
    void broadcastsToTheRoomLeftBehind() throws Exception {
        StompSession host = connect(hostToken);
        BlockingQueue<String> frames = subscribe(host, "/topic/rooms/" + roomId);
        host.send("/app/rooms/%d/enter".formatted(roomId), new byte[0]);
        next(frames);

        joinRoom(guestToken, createRoom(strangerToken, "옮겨갈 방"));

        String state = next(frames);
        assertThat((List<String>) JsonPath.read(state, "$.players[*].name")).containsExactly("김성호");
        host.disconnect();
    }
}
