package com.ksh.companybackend.user.application;

import com.ksh.companybackend.common.error.BusinessException;

public class TokenExpiredException extends BusinessException {

    public TokenExpiredException() {
        super("TOKEN_EXPIRED", 401, "인증이 만료되었습니다.");
    }
}
