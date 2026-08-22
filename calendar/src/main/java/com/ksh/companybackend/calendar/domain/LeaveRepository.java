package com.ksh.companybackend.calendar.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface LeaveRepository extends Repository<Leave, Long> {

    Optional<Leave> findById(Long id);

    @Query("select l from Leave l where l.endDate >= :from and l.startDate <= :to")
    List<Leave> findAllActiveBetween(LocalDate from, LocalDate to);

    @Query("select l from Leave l where l.userId = :userId and l.endDate >= :from and l.startDate <= :to")
    List<Leave> findAllActiveBetween(Long userId, LocalDate from, LocalDate to);

    Leave save(Leave leave);

    void delete(Leave leave);
}
