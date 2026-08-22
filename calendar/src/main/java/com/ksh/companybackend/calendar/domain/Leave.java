package com.ksh.companybackend.calendar.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "leaves")
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

    @ElementCollection
    @CollectionTable(
            name = "leave_days",
            joinColumns = @JoinColumn(name = "leave_id", foreignKey = @ForeignKey(name = "fk_leave_days_leave")),
            uniqueConstraints = @UniqueConstraint(name = "uk_leave_days_user_date", columnNames = {"user_id", "date"}))
    private Set<LeaveDay> days;

    private Leave(Long userId, LeaveKind kind, DateRange period) {
        this.userId = userId;
        this.kind = kind;
        this.startDate = period.from();
        this.endDate = period.to();
        this.createdAt = Instant.now();
        this.days = period.dates().stream()
                .map(date -> new LeaveDay(userId, date))
                .collect(Collectors.toSet());
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

    public boolean belongsTo(Long userId) {
        return this.userId.equals(userId);
    }
}
