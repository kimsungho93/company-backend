package com.ksh.companybackend.game.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class RoomFullException extends BusinessException {

    public RoomFullException() {
        super("ROOM_FULL", 409, "방이 가득 찼습니다.");
    }
}
