package com.ksh.companybackend.calendar.application;

import com.ksh.companybackend.calendar.application.dto.LeaveDetail;
import com.ksh.companybackend.calendar.domain.DateRange;
import com.ksh.companybackend.calendar.domain.Leave;
import com.ksh.companybackend.calendar.domain.LeaveKind;
import com.ksh.companybackend.calendar.domain.LeaveOverlapException;
import com.ksh.companybackend.calendar.domain.LeaveRepository;
import com.ksh.companybackend.calendar.domain.UserDirectory;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final UserDirectory userDirectory;

    public LeaveService(LeaveRepository leaveRepository, UserDirectory userDirectory) {
        this.leaveRepository = leaveRepository;
        this.userDirectory = userDirectory;
    }

    @Transactional
    public LeaveDetail create(Long userId, LeaveKind kind, DateRange period) {
        Leave leave = Leave.of(userId, kind, period);
        verifyNoOverlap(leave);

        return LeaveDetail.of(saveOrReportOverlap(leave), userDirectory.nameOf(userId));
    }

    @Transactional(readOnly = true)
    public List<LeaveDetail> findAllActiveBetween(DateRange window) {
        List<Leave> leaves = leaveRepository.findAllActiveBetween(window.from(), window.to());
        Map<Long, String> names = userDirectory.namesOf(
                leaves.stream().map(Leave::getUserId).distinct().toList());

        return leaves.stream()
                .map(leave -> LeaveDetail.of(leave, names.get(leave.getUserId())))
                .toList();
    }

    private Leave saveOrReportOverlap(Leave leave) {
        try {
            return leaveRepository.saveAndFlush(leave);
        } catch (DataIntegrityViolationException e) {
            throw new LeaveOverlapException(leave.period().from());
        }
    }

    private void verifyNoOverlap(Leave leave) {
        DateRange period = leave.period();

        leaveRepository.findAllActiveBetween(leave.getUserId(), period.from(), period.to()).stream()
                .findFirst()
                .ifPresent(conflicting -> {
                    throw new LeaveOverlapException(conflicting);
                });
    }
}
