package com.ksh.companybackend.game.api;

import com.ksh.companybackend.game.api.dto.RoomCreateRequest;
import com.ksh.companybackend.game.api.dto.RoomJoinRequest;
import com.ksh.companybackend.game.api.dto.RoomResponse;
import com.ksh.companybackend.game.application.RoomService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public List<RoomResponse> findAll() {
        return roomService.findAll().stream()
                .map(RoomResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@AuthenticationPrincipal Long callerId, @Valid @RequestBody RoomCreateRequest request) {
        return RoomResponse.from(roomService.open(callerId, request.name(), request.password()));
    }

    @PostMapping("/{id}/join")
    public RoomResponse join(@AuthenticationPrincipal Long callerId, @PathVariable Long id,
            @RequestBody(required = false) RoomJoinRequest request) {
        return RoomResponse.from(roomService.join(callerId, id, request == null ? null : request.password()));
    }
}
