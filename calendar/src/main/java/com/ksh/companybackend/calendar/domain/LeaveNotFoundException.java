package com.ksh.companybackend.calendar.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class LeaveNotFoundException extends BusinessException {

    public LeaveNotFoundException() {
        super("LEAVE_NOT_FOUND", 404, "휴가를 찾을 수 없습니다.");
    }
}
