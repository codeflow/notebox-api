package com.notebox.api.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A new organization and the administrator who will run it, created in one act.
 *
 * <p>Notebox is multi-tenant but had no way to create a tenant: members were provisioned by an
 * administrator (OQ-11) and the first administrator of a brand new organization had nobody to
 * provision them. This closes that loop.
 */
public record OrganizationSignup(
        @NotBlank(message = "organization.name.required")
        @Size(max = 120) String organizationName,
        @NotBlank(message = "organization.slug.required")
        @Pattern(regexp = "[a-z0-9]([a-z0-9-]{1,58}[a-z0-9])?", message = "organization.slug.invalid")
        String slug,
        @NotBlank(message = "member.displayName.required")
        @Size(max = 120) String displayName,
        @NotBlank(message = "member.email.required")
        @Email(message = "member.email.invalid")
        @Size(max = 254) String email,
        @NotBlank(message = "member.password.too_short")
        @Size(min = 10, max = 200, message = "member.password.too_short") String password) {
}
