package com.notebox.api.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Login credentials (AD-07 validation at the edge). */
public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank String password) {
}
