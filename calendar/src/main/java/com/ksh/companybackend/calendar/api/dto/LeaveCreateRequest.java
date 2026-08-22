package com.ksh.companybackend.calendar.api.dto;

import com.ksh.companybackend.calendar.domain.DateRange;
import com.ksh.companybackend.calendar.domain.LeaveKind;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LeaveCreateRequest(
        @NotNull(message = "휴가 종류를 선택해 주세요.")
        LeaveKind kind,

        @NotNull(message = "시작일을 입력해 주세요.")
        LocalDate startDate,

        @NotNull(message = "종료일을 입력해 주세요.")
        LocalDate endDate) {

    public DateRange period() {
        return new DateRange(startDate, endDate);
    }
}
