package com.ksh.companybackend.calendar.application;

import com.ksh.companybackend.calendar.application.dto.LeaveDetail;
import com.ksh.companybackend.calendar.domain.DateRange;
import com.ksh.companybackend.calendar.domain.Leave;
import com.ksh.companybackend.calendar.domain.LeaveKind;
import com.ksh.companybackend.calendar.domain.LeaveRepository;
import com.ksh.companybackend.calendar.domain.UserDirectory;
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
        Leave saved = leaveRepository.save(Leave.of(userId, kind, period));

        return new LeaveDetail(
                saved.getId(),
                userId,
                userDirectory.nameOf(userId),
                saved.getKind(),
                saved.period().from(),
                saved.period().to());
    }
}
