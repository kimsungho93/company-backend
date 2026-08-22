package com.ksh.companybackend.calendar.api;

import com.ksh.companybackend.calendar.api.dto.LeaveCreateRequest;
import com.ksh.companybackend.calendar.api.dto.LeaveResponse;
import com.ksh.companybackend.calendar.api.dto.LeaveSearchRequest;
import com.ksh.companybackend.calendar.application.LeaveService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    public List<LeaveResponse> findAll(@Valid LeaveSearchRequest request) {
        return leaveService.findAllActiveBetween(request.window()).stream()
                .map(LeaveResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse create(@AuthenticationPrincipal Long callerId, @Valid @RequestBody LeaveCreateRequest request) {
        return LeaveResponse.from(leaveService.create(callerId, request.kind(), request.period()));
    }
}
