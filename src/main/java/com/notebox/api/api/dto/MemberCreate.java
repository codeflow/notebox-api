package com.notebox.api.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A new member, provisioned by an administrator (OQ-11 — there is no self-service sign-up).
 *
 * <p>The tenant is NOT part of this payload on purpose: it comes from the caller's token, so an
 * administrator cannot provision into another workspace (BR-01, C-01).
 */
public record MemberCreate(
        @NotBlank(message = "member.email.required")
        @Email(message = "member.email.invalid")
        @Size(max = 254) String email,
        @NotBlank(message = "member.displayName.required")
        @Size(max = 120) String displayName,
        @Pattern(regexp = "MEMBER|ADMIN", message = "member.role.invalid") String role,
        @NotBlank(message = "member.password.too_short")
        @Size(min = 10, max = 200, message = "member.password.too_short") String password) {
}
