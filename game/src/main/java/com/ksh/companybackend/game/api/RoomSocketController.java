package com.ksh.companybackend.game.api;

import com.ksh.companybackend.common.error.BusinessException;
import com.ksh.companybackend.common.error.ErrorResponse;
import com.ksh.companybackend.game.api.dto.AvatarRequest;
import com.ksh.companybackend.game.api.dto.ReadyRequest;
import com.ksh.companybackend.game.api.dto.TransferRequest;
import com.ksh.companybackend.game.application.RoomService;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class RoomSocketController {

    private final RoomService roomService;

    public RoomSocketController(RoomService roomService) {
        this.roomService = roomService;
    }

    @MessageMapping("/rooms/{id}/enter")
    public void enter(@DestinationVariable Long id, Principal caller, SimpMessageHeaderAccessor headers) {
        roomService.enter(callerId(caller), id, headers.getSessionId());
    }

    @MessageMapping("/rooms/{id}/avatar")
    public void avatar(@DestinationVariable Long id, Principal caller, AvatarRequest request) {
        roomService.changeAvatar(callerId(caller), id, request.avatar());
    }

    @MessageMapping("/rooms/{id}/ready")
    public void ready(@DestinationVariable Long id, Principal caller, ReadyRequest request) {
        roomService.changeReady(callerId(caller), id, request.ready());
    }

    @MessageMapping("/rooms/{id}/transfer")
    public void transfer(@DestinationVariable Long id, Principal caller, TransferRequest request) {
        roomService.transfer(callerId(caller), id, request.userId());
    }

    @MessageMapping("/rooms/{id}/start")
    public void start(@DestinationVariable Long id, Principal caller) {
        roomService.start(callerId(caller), id);
    }

    @MessageMapping("/rooms/{id}/leave")
    public void leave(@DestinationVariable Long id, Principal caller) {
        roomService.leave(callerId(caller), id);
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handle(BusinessException e) {
        return new ErrorResponse(e.getCode(), e.getMessage());
    }

    private Long callerId(Principal caller) {
        return (Long) ((Authentication) caller).getPrincipal();
    }
}
