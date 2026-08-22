package com.ksh.companybackend.calendar.api.dto;

import com.ksh.companybackend.calendar.domain.DateRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HolidayCreateRequest(
        @NotBlank(message = "공휴일 이름을 입력해 주세요.")
        @Size(max = 12, message = "공휴일 이름은 12자까지 입력할 수 있습니다.")
        String name,

        @NotNull(message = "시작일을 입력해 주세요.")
        LocalDate startDate,

        @NotNull(message = "종료일을 입력해 주세요.")
        LocalDate endDate) {

    public DateRange period() {
        return new DateRange(startDate, endDate);
    }
}
