package com.ksh.companybackend.user.application.dto;

import com.ksh.companybackend.user.domain.Role;
import com.ksh.companybackend.user.domain.UserStatus;

public record UserProfile(Long id, String email, String name, Role role, UserStatus status) {
}
