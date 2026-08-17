package com.ksh.companybackend.user.application;

import com.ksh.companybackend.common.error.BusinessException;

public class UnauthenticatedException extends BusinessException {

    public UnauthenticatedException() {
        super("UNAUTHENTICATED", 401, "로그인이 필요합니다.");
    }
}
