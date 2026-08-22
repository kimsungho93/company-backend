package com.ksh.companybackend.calendar.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LeaveTest {

    private static final LocalDate MON = LocalDate.of(2026, 8, 17);

    private DateRange days(int count) {
        return new DateRange(MON, MON.plusDays(count - 1));
    }

    @Test
    @DisplayName("연차는 여러 날에 걸칠 수 있다")
    void annualSpansDays() {
        Leave leave = Leave.of(1L, LeaveKind.ANNUAL, days(4));

        assertThat(leave.period()).isEqualTo(new DateRange(MON, MON.plusDays(3)));
    }

    @Test
    @DisplayName("반차는 하루만 가능하다 - 반나절이라 기간이 성립하지 않는다")
    void rejectsMultiDayHalfDay() {
        assertThatThrownBy(() -> Leave.of(1L, LeaveKind.HALF_DAY_AM, days(2)))
                .isInstanceOf(HalfDayMustBeSingleDayException.class);

        assertThatThrownBy(() -> Leave.of(1L, LeaveKind.HALF_DAY_PM, days(2)))
                .isInstanceOf(HalfDayMustBeSingleDayException.class);
    }

    @Test
    @DisplayName("반차도 하루면 통과한다")
    void allowsSingleDayHalfDay() {
        assertThat(Leave.of(1L, LeaveKind.HALF_DAY_AM, days(1)).getKind()).isEqualTo(LeaveKind.HALF_DAY_AM);
    }

    @Test
    @DisplayName("366일까지는 되고 367일부터 거부한다")
    void rejectsRangeLongerThanAYear() {
        assertThat(Leave.of(1L, LeaveKind.ANNUAL, days(366))).isNotNull();

        assertThatThrownBy(() -> Leave.of(1L, LeaveKind.ANNUAL, days(367)))
                .isInstanceOf(LeaveRangeTooLongException.class);
    }

    @Test
    @DisplayName("기간에 걸치는지 스스로 판단한다")
    void knowsWhetherItIsActive() {
        Leave leave = Leave.of(1L, LeaveKind.ANNUAL, days(4));

        assertThat(leave.isActiveBetween(new DateRange(MON.plusDays(3), MON.plusDays(5)))).isTrue();
        assertThat(leave.isActiveBetween(new DateRange(MON.plusDays(4), MON.plusDays(5)))).isFalse();
    }

    @Test
    @DisplayName("본인 것인지 안다")
    void knowsItsOwner() {
        Leave leave = Leave.of(7L, LeaveKind.ANNUAL, days(1));

        assertThat(leave.belongsTo(7L)).isTrue();
        assertThat(leave.belongsTo(8L)).isFalse();
    }
}
