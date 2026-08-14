package com.ksh.companybackend.user.application;

import com.ksh.companybackend.common.error.BusinessException;

public class EmailAlreadyExistsException extends BusinessException {

    public EmailAlreadyExistsException() {
        super("EMAIL_ALREADY_EXISTS", 409, "이미 가입된 이메일입니다.");
    }
}
