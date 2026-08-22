package com.ksh.companybackend.game.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class RoomNotFoundException extends BusinessException {

    public RoomNotFoundException() {
        super("ROOM_NOT_FOUND", 404, "방을 찾을 수 없습니다.");
    }
}
