package com.ksh.companybackend.calendar.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class NotAdminException extends BusinessException {

    public NotAdminException() {
        super("FORBIDDEN", 403, "관리자만 공휴일을 관리할 수 있습니다.");
    }
}
