package com.ksh.companybackend.game.api.dto;

import com.ksh.companybackend.game.application.dto.RoomSummary;
import com.ksh.companybackend.game.domain.RoomStatus;

public record RoomResponse(
        Long id, String name, String hostName, int playerCount, int capacity, boolean locked, RoomStatus status) {

    public static RoomResponse from(RoomSummary summary) {
        return new RoomResponse(
                summary.id(), summary.name(), summary.hostName(), summary.playerCount(),
                summary.capacity(), summary.locked(), summary.status());
    }
}
