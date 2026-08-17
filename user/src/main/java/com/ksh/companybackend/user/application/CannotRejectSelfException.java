package com.ksh.companybackend.user.application;

import com.ksh.companybackend.common.error.BusinessException;

public class CannotRejectSelfException extends BusinessException {

    public CannotRejectSelfException() {
        super("CANNOT_REJECT_SELF", 400, "자기 자신은 거절할 수 없습니다.");
    }
}
