package com.ksh.companybackend.calendar.application.dto;

import com.ksh.companybackend.calendar.domain.Holiday;
import java.time.LocalDate;

public record HolidayDetail(Long id, String name, LocalDate startDate, LocalDate endDate) {

    public static HolidayDetail of(Holiday holiday) {
        return new HolidayDetail(
                holiday.getId(), holiday.getName(), holiday.period().from(), holiday.period().to());
    }
}
