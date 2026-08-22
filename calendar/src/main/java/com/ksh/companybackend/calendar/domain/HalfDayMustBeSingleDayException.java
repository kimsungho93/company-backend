package com.ksh.companybackend.calendar.domain;

import com.ksh.companybackend.common.error.BusinessException;

public class HalfDayMustBeSingleDayException extends BusinessException {

    public HalfDayMustBeSingleDayException() {
        super("HALF_DAY_MUST_BE_SINGLE_DAY", 400, "반차는 하루만 낼 수 있습니다.");
    }
}
