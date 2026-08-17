package com.ksh.companybackend.user.application;

import com.ksh.companybackend.common.error.BusinessException;

public class ForbiddenException extends BusinessException {

    public ForbiddenException() {
        super("FORBIDDEN", 403, "권한이 없습니다.");
    }
}
