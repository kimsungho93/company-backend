package com.ksh.companybackend.user.application;

import com.ksh.companybackend.common.error.BusinessException;

public class InvalidTokenException extends BusinessException {

    public InvalidTokenException() {
        super("INVALID_TOKEN", 401, "유효하지 않은 인증입니다.");
    }
}
