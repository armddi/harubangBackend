package com.harubang.harubangBackend.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
        @NotBlank @Email String email
) {}
