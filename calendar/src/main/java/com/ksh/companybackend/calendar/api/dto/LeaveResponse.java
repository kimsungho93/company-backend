package com.ksh.companybackend.calendar.api.dto;

import com.ksh.companybackend.calendar.application.dto.LeaveDetail;
import com.ksh.companybackend.calendar.domain.LeaveKind;
import java.time.LocalDate;

public record LeaveResponse(Long id, Long userId, String name, LeaveKind kind, LocalDate startDate, LocalDate endDate) {

    public static LeaveResponse from(LeaveDetail detail) {
        return new LeaveResponse(
                detail.id(), detail.userId(), detail.name(), detail.kind(), detail.startDate(), detail.endDate());
    }
}
