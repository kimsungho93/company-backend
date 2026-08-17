package com.ksh.companybackend.user.application.dto;

import com.ksh.companybackend.user.domain.UserStatus;
import java.time.Instant;

public record AdminUserSummary(Long id, String email, String name, UserStatus status, Instant createdAt) {
}
