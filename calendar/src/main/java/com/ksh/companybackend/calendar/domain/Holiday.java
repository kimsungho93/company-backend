package com.ksh.companybackend.calendar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "holidays")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Getter
    @Column(nullable = false, length = 12, updatable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    private LocalDate startDate;

    @Column(nullable = false, updatable = false)
    private LocalDate endDate;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Holiday(String name, DateRange period, Long createdBy) {
        this.name = name;
        this.startDate = period.from();
        this.endDate = period.to();
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public static Holiday of(String name, DateRange period, Long createdBy) {
        return new Holiday(name, period, createdBy);
    }

    public DateRange period() {
        return new DateRange(startDate, endDate);
    }
}
