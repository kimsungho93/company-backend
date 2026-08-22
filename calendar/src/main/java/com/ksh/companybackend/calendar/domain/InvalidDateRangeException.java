package com.ksh.companybackend.calendar.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class InvalidDateRangeException extends BusinessException {

    public InvalidDateRangeException() {
        super("INVALID_DATE_RANGE", 400, "종료일이 시작일보다 이를 수 없습니다.");
    }
}
