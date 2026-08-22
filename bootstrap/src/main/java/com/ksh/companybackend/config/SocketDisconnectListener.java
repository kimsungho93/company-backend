package com.ksh.companybackend.config;

import com.ksh.companybackend.game.application.RoomService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

// 브라우저를 닫아도 방에 남으면 유령 인원이 정원을 먹어 10명짜리 방이 금세 못 쓰게 된다.
@Component
public class SocketDisconnectListener {

    private final RoomService roomService;

    public SocketDisconnectListener(RoomService roomService) {
        this.roomService = roomService;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        roomService.leaveBySession(event.getSessionId());
    }
}
