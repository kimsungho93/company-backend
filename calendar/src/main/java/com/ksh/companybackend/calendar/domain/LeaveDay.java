package com.ksh.companybackend.calendar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
public record LeaveDay(
        @Column(name = "user_id", nullable = false, updatable = false) Long userId,
        @Column(name = "date", nullable = false, updatable = false) LocalDate date) {
}
