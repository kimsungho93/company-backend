package com.ksh.companybackend.user.application;

import com.ksh.companybackend.common.error.BusinessException;

public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", 401, "이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
