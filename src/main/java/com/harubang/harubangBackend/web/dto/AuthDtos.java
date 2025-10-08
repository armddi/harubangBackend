package com.harubang.harubangBackend.web.dto;

import jakarta.validation.constraints.*;
public class AuthDtos {
    public record SignupRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min=2,max=50) String name,
            @NotBlank @Size(min=6,max=100) String password
    ) {}
    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    // 이메일 인증 “요청”용 (사전등록)
    public record RequestVerificationRequest(
            @Email @NotBlank String email
    ) {}

    public record TokenResponse(String accessToken) {}
}
