package com.ksh.companybackend.calendar.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class NotLeaveOwnerException extends BusinessException {

    public NotLeaveOwnerException() {
        super("FORBIDDEN", 403, "본인의 휴가만 지울 수 있습니다.");
    }
}
