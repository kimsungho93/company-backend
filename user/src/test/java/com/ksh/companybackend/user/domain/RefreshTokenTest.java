package com.ksh.companybackend.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final Duration TTL = Duration.ofDays(14);

    @Test
    @DisplayName("발급하면 원문이 아니라 해시를 담는다")
    void storesHashNotRawValue() {
        RefreshToken.Issued issued = RefreshToken.issue(42L, TTL);

        assertThat(issued.rawValue()).isNotBlank();
        assertThat(issued.token().getTokenHash())
                .isNotEqualTo(issued.rawValue())
                .isEqualTo(RefreshToken.hash(issued.rawValue()));
    }

    @Test
    @DisplayName("발급할 때마다 원문이 다르다")
    void issuesDifferentValueEachTime() {
        assertThat(RefreshToken.issue(42L, TTL).rawValue())
                .isNotEqualTo(RefreshToken.issue(42L, TTL).rawValue());
    }

    @Test
    @DisplayName("수명이 지난 토큰은 만료로 본다")
    void expiredWhenPastTtl() {
        assertThat(RefreshToken.issue(42L, TTL).token().isExpired()).isFalse();
        assertThat(RefreshToken.issue(42L, Duration.ofMinutes(-1)).token().isExpired()).isTrue();
    }

    @Test
    @DisplayName("폐기하면 폐기 상태가 된다")
    void revokes() {
        RefreshToken token = RefreshToken.issue(42L, TTL).token();
        assertThat(token.isRevoked()).isFalse();

        token.revoke();

        assertThat(token.isRevoked()).isTrue();
    }
}
