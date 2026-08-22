package com.ksh.companybackend;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class WebSocketProbeIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("SockJS 없이 순수 WebSocket 으로 붙어 에코가 돌아온다")
    void echoesOverRawWebSocket() throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new StringMessageConverter());

        StompSession session = client
                .connectAsync("ws://localhost:%d/api/ws".formatted(port), new StompSessionHandlerAdapter() { })
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/echo", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((String) payload);
            }
        });

        session.send("/app/echo", "핑");

        assertThat(received.poll(5, TimeUnit.SECONDS)).isEqualTo("핑");
    }
}
