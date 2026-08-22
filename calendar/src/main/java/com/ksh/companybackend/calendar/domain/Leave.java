package com.ksh.companybackend.calendar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "leaves",
        uniqueConstraints = @UniqueConstraint(name = "uk_leaves_user_start", columnNames = {"user_id", "start_date"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Leave {

    private static final int MAX_DAYS = 366;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Getter
    @Column(nullable = false, updatable = false)
    private Long userId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private LeaveKind kind;

    @Column(nullable = false, updatable = false)
    private LocalDate startDate;

    @Column(nullable = false, updatable = false)
    private LocalDate endDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Leave(Long userId, LeaveKind kind, DateRange period) {
        this.userId = userId;
        this.kind = kind;
        this.startDate = period.from();
        this.endDate = period.to();
        this.createdAt = Instant.now();
    }

    public static Leave of(Long userId, LeaveKind kind, DateRange period) {
        if (kind.isHalfDay() && !period.isSingleDay()) {
            throw new HalfDayMustBeSingleDayException();
        }
        if (period.days() > MAX_DAYS) {
            throw new LeaveRangeTooLongException();
        }

        return new Leave(userId, kind, period);
    }

    public DateRange period() {
        return new DateRange(startDate, endDate);
    }

    public boolean isActiveBetween(DateRange window) {
        return period().overlaps(window);
    }

    public boolean belongsTo(Long userId) {
        return this.userId.equals(userId);
    }
}
