package com.ksh.companybackend.config;

import com.ksh.companybackend.game.application.RoomService;
import com.ksh.companybackend.game.domain.NotInRoomException;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

// STOMP 는 SUBSCRIBE 를 검사하지 않는다. 막지 않으면 소켓만 붙일 수 있는 사람이
// 남의 비밀방을 계속 엿볼 수 있고, POST /join 의 비밀번호 검사가 무의미해진다.
@Component
public class RoomSubscriptionInterceptor implements ChannelInterceptor {

    private static final Pattern ROOM_TOPIC = Pattern.compile("^/topic/rooms/(\\d+)$");

    private final RoomService roomService;

    public RoomSubscriptionInterceptor(RoomService roomService) {
        this.roomService = roomService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        Matcher room = ROOM_TOPIC.matcher(String.valueOf(accessor.getDestination()));
        if (room.matches() && !roomService.isParticipant(Long.valueOf(room.group(1)), callerId(accessor.getUser()))) {
            throw new NotInRoomException();
        }

        return message;
    }

    private Long callerId(Principal caller) {
        return caller instanceof Authentication authentication ? (Long) authentication.getPrincipal() : null;
    }
}
