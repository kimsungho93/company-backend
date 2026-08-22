package com.ksh.companybackend.game.api;

import com.ksh.companybackend.game.api.dto.RoomResponse;
import com.ksh.companybackend.game.api.dto.RoomStateResponse;
import com.ksh.companybackend.game.application.RoomBroadcaster;
import com.ksh.companybackend.game.application.dto.RoomSummary;
import com.ksh.companybackend.game.domain.Room;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompRoomBroadcaster implements RoomBroadcaster {

    // 템플릿을 생성자에서 바로 받으면 순환이 된다 - 이 빈은 브로커 설정이 만드는데,
    // 그 설정이 구독 인터셉터를, 인터셉터가 RoomService 를, 서비스가 다시 이 빈을 부른다.
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplate;

    public StompRoomBroadcaster(ObjectProvider<SimpMessagingTemplate> messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void roomChanged(Room room) {
        send("/topic/rooms/" + room.id(), RoomStateResponse.from(room));
    }

    @Override
    public void roomListChanged(List<RoomSummary> rooms) {
        send("/topic/rooms", rooms.stream().map(RoomResponse::from).toList());
    }

    private void send(String destination, Object payload) {
        messagingTemplate.getObject().convertAndSend(destination, payload);
    }
}
