package com.ksh.companybackend.user.api.dto;

import com.ksh.companybackend.user.domain.UserStatus;
import java.time.Instant;

public record AdminUserResponse(Long id, String email, String name, UserStatus status, Instant createdAt) {
}
