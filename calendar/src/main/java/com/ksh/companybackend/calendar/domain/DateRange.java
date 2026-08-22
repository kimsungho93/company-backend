package com.ksh.companybackend.calendar.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record DateRange(LocalDate from, LocalDate to) {

    public DateRange {
        if (to.isBefore(from)) {
            throw new InvalidDateRangeException();
        }
    }

    public boolean isSingleDay() {
        return from.equals(to);
    }

    public long days() {
        return ChronoUnit.DAYS.between(from, to) + 1;
    }
}
