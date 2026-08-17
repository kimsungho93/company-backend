package com.ksh.companybackend.user.api.dto;

import com.ksh.companybackend.user.domain.Role;
import com.ksh.companybackend.user.domain.UserStatus;

public record MeResponse(Long id, String email, String name, Role role, UserStatus status) {
}
