package com.ksh.companybackend.user.api.dto;


public record LoginResponse(String accessToken, long expiresIn) {
}
