package com.ksh.companybackend.game.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class WrongRoomPasswordException extends BusinessException {

    public WrongRoomPasswordException() {
        super("WRONG_ROOM_PASSWORD", 403, "비밀번호가 맞지 않습니다.");
    }
}
