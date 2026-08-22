package com.ksh.companybackend.calendar.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class LeaveRangeTooLongException extends BusinessException {

    public LeaveRangeTooLongException() {
        super("LEAVE_RANGE_TOO_LONG", 400, "휴가 기간이 너무 깁니다.");
    }
}
