package com.ksh.companybackend.game.api.dto;

import com.ksh.companybackend.game.domain.Room;
import com.ksh.companybackend.game.domain.RoomStatus;
import java.util.List;

public record RoomStateResponse(
        Long id, String name, RoomStatus status, Long hostId, int capacity, List<PlayerResponse> players) {

    public static RoomStateResponse from(Room room) {
        return new RoomStateResponse(
                room.id(), room.name(), room.status(), room.hostId(), room.capacity(),
                room.players().stream().map(PlayerResponse::from).toList());
    }
}
