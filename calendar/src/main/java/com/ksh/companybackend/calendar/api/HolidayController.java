package com.ksh.companybackend.calendar.api;

import com.ksh.companybackend.calendar.api.dto.DateWindowRequest;
import com.ksh.companybackend.calendar.api.dto.HolidayCreateRequest;
import com.ksh.companybackend.calendar.api.dto.HolidayResponse;
import com.ksh.companybackend.calendar.application.HolidayService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping
    public List<HolidayResponse> findAll(@Valid DateWindowRequest request) {
        return holidayService.findAllActiveBetween(request.window()).stream()
                .map(HolidayResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HolidayResponse create(@AuthenticationPrincipal Long callerId,
            @Valid @RequestBody HolidayCreateRequest request) {
        return HolidayResponse.from(holidayService.create(callerId, request.name(), request.period()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long callerId, @PathVariable Long id) {
        holidayService.delete(callerId, id);
    }
}
