// src/main/java/com/boot/ict05_final_user/domain/auth/dto/LoginRequest.java
package com.boot.ict05_final_user.config.security.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String loginType // "HQ" | "Store"
) {}
