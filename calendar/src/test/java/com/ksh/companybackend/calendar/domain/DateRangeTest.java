package com.ksh.companybackend.calendar.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DateRangeTest {

    private static final LocalDate MON = LocalDate.of(2026, 8, 17);

    @Test
    @DisplayName("끝이 시작보다 이르면 만들 수 없다")
    void rejectsReversed() {
        assertThatThrownBy(() -> new DateRange(MON, MON.minusDays(1)))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    @DisplayName("하루짜리도 유효하다")
    void allowsSingleDay() {
        assertThat(new DateRange(MON, MON).isSingleDay()).isTrue();
    }

    @Test
    @DisplayName("일수는 양끝을 포함해서 센다")
    void countsBothEnds() {
        assertThat(new DateRange(MON, MON).days()).isEqualTo(1);
        assertThat(new DateRange(MON, MON.plusDays(3)).days()).isEqualTo(4);
    }

    @Test
    @DisplayName("겹침은 양끝이 닿기만 해도 참이다")
    void overlapsIncludesBothEnds() {
        DateRange week = new DateRange(MON, MON.plusDays(3));

        assertThat(week.overlaps(new DateRange(MON.plusDays(3), MON.plusDays(5)))).isTrue();
        assertThat(week.overlaps(new DateRange(MON.minusDays(5), MON))).isTrue();
        assertThat(week.overlaps(new DateRange(MON.plusDays(4), MON.plusDays(5)))).isFalse();
        assertThat(week.overlaps(new DateRange(MON.minusDays(5), MON.minusDays(1)))).isFalse();
    }
}
