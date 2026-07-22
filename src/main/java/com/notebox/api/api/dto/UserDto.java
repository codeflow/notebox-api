package com.notebox.api.api.dto;

import com.notebox.api.domain.User;

/** User projection for API responses. The password hash is never included (INV-3, C-04). */
public record UserDto(
        String id,
        String tenantId,
        String email,
        String role,
        String displayName,
        String locale,
        boolean active) {

    public static UserDto from(User user) {
        return new UserDto(
                user.getId().toString(),
                user.getTenantId().toString(),
                user.getEmail(),
                user.getRole().name(),
                user.getDisplayName(),
                user.getLocalePreference(),
                user.isActive());
    }
}
