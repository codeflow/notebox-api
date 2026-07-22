package com.notebox.api.api.dto;

import com.notebox.api.domain.User;

/** The authenticated caller's own identity summary. */
public record MeDto(
        String userId,
        String tenantId,
        String role,
        String displayName,
        String locale) {

    public static MeDto from(User user) {
        return new MeDto(
                user.getId().toString(),
                user.getTenantId().toString(),
                user.getRole().name(),
                user.getDisplayName(),
                user.getLocalePreference());
    }
}
