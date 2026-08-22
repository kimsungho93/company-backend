package com.ksh.companybackend.game.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class NotAllReadyException extends BusinessException {

    public NotAllReadyException() {
        super("NOT_ALL_READY", 409, "아직 준비하지 않은 사람이 있습니다.");
    }
}
