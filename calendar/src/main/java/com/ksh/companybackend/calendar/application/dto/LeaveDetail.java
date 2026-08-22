package com.ksh.companybackend.calendar.application.dto;

import com.ksh.companybackend.calendar.domain.Leave;
import com.ksh.companybackend.calendar.domain.LeaveKind;
import java.time.LocalDate;

public record LeaveDetail(Long id, Long userId, String name, LeaveKind kind, LocalDate startDate, LocalDate endDate) {

    public static LeaveDetail of(Leave leave, String name) {
        return new LeaveDetail(
                leave.getId(),
                leave.getUserId(),
                name,
                leave.getKind(),
                leave.period().from(),
                leave.period().to());
    }
}
