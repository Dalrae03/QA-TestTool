package com.tms.user.dto;

import com.tms.user.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Email
        @Size(max = 200)
        String email,

        @NotNull
        UserRole role,

        Boolean active
) {
}
