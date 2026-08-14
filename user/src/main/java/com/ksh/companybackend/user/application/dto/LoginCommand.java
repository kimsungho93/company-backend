package com.ksh.companybackend.user.application.dto;

public record LoginCommand(String email, String rawPassword) {
}
