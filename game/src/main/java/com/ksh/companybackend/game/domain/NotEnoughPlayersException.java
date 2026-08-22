package com.ksh.companybackend.game.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class NotEnoughPlayersException extends BusinessException {

    public NotEnoughPlayersException() {
        super("NOT_ENOUGH_PLAYERS", 409, "두 명 이상이어야 시작할 수 있습니다.");
    }
}
