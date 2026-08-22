package com.ksh.companybackend.game.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class NotRoomHostException extends BusinessException {

    public NotRoomHostException() {
        super("NOT_ROOM_HOST", 403, "방장만 할 수 있습니다.");
    }
}
