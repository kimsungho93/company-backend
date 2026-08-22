package com.ksh.companybackend.calendar.domain;

import com.ksh.companybackend.common.error.BusinessException;
import java.time.LocalDate;

public class LeaveOverlapException extends BusinessException {

    public LeaveOverlapException(Leave conflicting) {
        super("LEAVE_OVERLAP", 409, describe(conflicting.period().from(), conflicting.getKind().label()));
    }

    public LeaveOverlapException(LocalDate startDate) {
        super("LEAVE_OVERLAP", 409, describe(startDate, "휴가"));
    }

    private static String describe(LocalDate start, String what) {
        return "%d월 %d일부터 %s가 이미 있습니다. 기존 휴가를 지우고 다시 등록해 주세요."
                .formatted(start.getMonthValue(), start.getDayOfMonth(), what);
    }
}
