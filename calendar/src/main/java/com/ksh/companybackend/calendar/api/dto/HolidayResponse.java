package com.ksh.companybackend.calendar.api.dto;

import com.ksh.companybackend.calendar.application.dto.HolidayDetail;
import java.time.LocalDate;

public record HolidayResponse(Long id, String name, LocalDate startDate, LocalDate endDate) {

    public static HolidayResponse from(HolidayDetail detail) {
        return new HolidayResponse(detail.id(), detail.name(), detail.startDate(), detail.endDate());
    }
}
