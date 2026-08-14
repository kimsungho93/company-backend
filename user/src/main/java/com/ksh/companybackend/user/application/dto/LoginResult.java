package com.ksh.companybackend.user.application.dto;

public record LoginResult(String accessToken, long expiresIn) {
}
