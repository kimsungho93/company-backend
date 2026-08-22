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
    @DisplayName("기간이 덮는 날짜를 양끝 포함해 펼친다")
    void expandsToDates() {
        assertThat(new DateRange(MON, MON.plusDays(2)).dates())
                .containsExactly(MON, MON.plusDays(1), MON.plusDays(2));
        assertThat(new DateRange(MON, MON).dates()).containsExactly(MON);
    }
}
