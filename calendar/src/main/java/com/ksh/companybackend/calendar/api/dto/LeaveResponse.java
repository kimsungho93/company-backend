package com.ksh.companybackend.calendar.api.dto;

import com.ksh.companybackend.calendar.domain.LeaveKind;
import java.time.LocalDate;

public record LeaveResponse(Long id, Long userId, String name, LeaveKind kind, LocalDate startDate, LocalDate endDate) {
}
