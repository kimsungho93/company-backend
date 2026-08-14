package com.ksh.companybackend.user.application.dto;

public record SignupCommand(String email, String name, String rawPassword) {
}
