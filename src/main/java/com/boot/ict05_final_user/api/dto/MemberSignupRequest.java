// src/main/java/com/boot/ict05_final_user/api/dto/MemberSignupRequest.java
package com.boot.ict05_final_user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MemberSignupRequest {
    @NotBlank
    private String name;

    @Email @NotBlank
    private String email;

    private String phone;

    @NotBlank
    private String password;
}
