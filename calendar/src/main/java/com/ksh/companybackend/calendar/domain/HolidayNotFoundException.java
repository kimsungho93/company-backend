package com.ksh.companybackend.calendar.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class HolidayNotFoundException extends BusinessException {

    public HolidayNotFoundException() {
        super("HOLIDAY_NOT_FOUND", 404, "공휴일을 찾을 수 없습니다.");
    }
}
