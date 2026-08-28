package com.notebox.api.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A password an administrator sets on a member's behalf. Never echoed back in any response. */
public record PasswordReset(
        @NotBlank(message = "member.password.too_short")
        @Size(min = 10, max = 200, message = "member.password.too_short") String password) {
}
