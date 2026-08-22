package com.ksh.companybackend.game.application.dto;

import com.ksh.companybackend.game.domain.Room;
import com.ksh.companybackend.game.domain.RoomStatus;

public record RoomSummary(
        Long id, String name, String hostName, int playerCount, int capacity, boolean locked, RoomStatus status) {

    public static RoomSummary of(Room room) {
        return new RoomSummary(
                room.id(), room.name(), room.hostName(), room.playerCount(),
                room.capacity(), room.isLocked(), room.status());
    }
}
