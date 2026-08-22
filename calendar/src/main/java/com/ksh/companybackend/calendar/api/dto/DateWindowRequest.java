package com.ksh.companybackend.calendar.api.dto;

import com.ksh.companybackend.calendar.domain.DateRange;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DateWindowRequest(
        @NotNull(message = "조회 시작일을 입력해 주세요.")
        LocalDate from,

        @NotNull(message = "조회 종료일을 입력해 주세요.")
        LocalDate to) {

    public DateRange window() {
        return new DateRange(from, to);
    }
}
