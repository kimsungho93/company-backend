package com.ksh.companybackend.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    private User newUser() {
        return User.create("tiger@ibslab.com", "encoded-password", "테스트");
    }

    @Test
    @DisplayName("가입 직후에는 승인 대기 상태다")
    void startsPending() {
        assertThat(newUser().getStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    @DisplayName("승인하면 APPROVED 가 된다")
    void approves() {
        User user = newUser();

        user.approve();

        assertThat(user.getStatus()).isEqualTo(UserStatus.APPROVED);
    }

    @Test
    @DisplayName("거절하면 REJECTED 가 된다")
    void rejects() {
        User user = newUser();

        user.reject();

        assertThat(user.getStatus()).isEqualTo(UserStatus.REJECTED);
    }

    @Test
    @DisplayName("가입 직후에는 관리자가 아니다")
    void isNotAdminByDefault() {
        assertThat(newUser().isAdmin()).isFalse();
    }

    @Test
    @DisplayName("관리자로 지정하면 isAdmin 이 참이다")
    void grantsAdmin() {
        User user = newUser();

        user.grantAdmin();

        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("거절한 뒤에도 다시 승인할 수 있다 - 실수를 행 삭제 없이 되돌린다")
    void approvesAfterReject() {
        User user = newUser();
        user.reject();

        user.approve();

        assertThat(user.getStatus()).isEqualTo(UserStatus.APPROVED);
    }
}
