package com.ksh.companybackend.user.application;

import com.ksh.companybackend.common.error.BusinessException;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException() {
        super("USER_NOT_FOUND", 404, "사용자를 찾을 수 없습니다.");
    }
}
