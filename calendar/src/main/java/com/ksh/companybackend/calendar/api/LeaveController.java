package com.ksh.companybackend.calendar.api;

import com.ksh.companybackend.calendar.api.dto.LeaveCreateRequest;
import com.ksh.companybackend.calendar.api.dto.LeaveResponse;
import com.ksh.companybackend.calendar.application.LeaveService;
import com.ksh.companybackend.calendar.application.dto.LeaveDetail;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse create(@AuthenticationPrincipal Long callerId, @Valid @RequestBody LeaveCreateRequest request) {
        LeaveDetail created = leaveService.create(callerId, request.kind(), request.period());

        return new LeaveResponse(
                created.id(), created.userId(), created.name(),
                created.kind(), created.startDate(), created.endDate());
    }
}
