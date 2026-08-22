package com.ksh.companybackend.game.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class NotInRoomException extends BusinessException {

    public NotInRoomException() {
        super("NOT_IN_ROOM", 403, "이 방의 참가자가 아닙니다.");
    }
}
