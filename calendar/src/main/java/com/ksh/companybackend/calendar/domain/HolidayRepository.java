package com.ksh.companybackend.calendar.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface HolidayRepository extends Repository<Holiday, Long> {

    Optional<Holiday> findById(Long id);

    @Query("select h from Holiday h where h.endDate >= :from and h.startDate <= :to")
    List<Holiday> findAllActiveBetween(LocalDate from, LocalDate to);

    Holiday save(Holiday holiday);

    void delete(Holiday holiday);
}
