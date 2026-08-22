package com.ksh.companybackend.calendar.application;

import com.ksh.companybackend.calendar.application.dto.HolidayDetail;
import com.ksh.companybackend.calendar.domain.DateRange;
import com.ksh.companybackend.calendar.domain.Holiday;
import com.ksh.companybackend.calendar.domain.HolidayNotFoundException;
import com.ksh.companybackend.calendar.domain.HolidayRepository;
import com.ksh.companybackend.calendar.domain.NotAdminException;
import com.ksh.companybackend.calendar.domain.UserRoles;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final UserRoles userRoles;

    public HolidayService(HolidayRepository holidayRepository, UserRoles userRoles) {
        this.holidayRepository = holidayRepository;
        this.userRoles = userRoles;
    }

    @Transactional(readOnly = true)
    public List<HolidayDetail> findAllActiveBetween(DateRange window) {
        return holidayRepository.findAllActiveBetween(window.from(), window.to()).stream()
                .map(HolidayDetail::of)
                .toList();
    }

    @Transactional
    public HolidayDetail create(Long callerId, String name, DateRange period) {
        verifyAdmin(callerId);

        return HolidayDetail.of(holidayRepository.save(Holiday.of(name, period, callerId)));
    }

    @Transactional
    public void delete(Long callerId, Long holidayId) {
        verifyAdmin(callerId);

        holidayRepository.delete(holidayRepository.findById(holidayId).orElseThrow(HolidayNotFoundException::new));
    }

    private void verifyAdmin(Long callerId) {
        if (!userRoles.isAdmin(callerId)) {
            throw new NotAdminException();
        }
    }
}
