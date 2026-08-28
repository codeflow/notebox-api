package com.notebox.api.api.dto;

import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.User;

/**
 * The authenticated caller's own identity summary.
 *
 * <p>{@code tenantName} and {@code tenantSlug} exist so the client can NAME the workspace instead
 * of printing its id: before they were carried here, {@code notebox-web} rendered the raw tenant
 * UUID in the chrome of every protected screen (OQ-30). They are the caller's own tenant only —
 * this endpoint never resolves any other one.
 */
public record MeDto(
        String userId,
        String tenantId,
        String tenantName,
        String tenantSlug,
        String role,
        String displayName,
        String locale) {

    public static MeDto from(User user, Tenant tenant) {
        return new MeDto(
                user.getId().toString(),
                user.getTenantId().toString(),
                tenant.getName(),
                tenant.getSlug(),
                user.getRole().name(),
                user.getDisplayName(),
                user.getLocalePreference());
    }
}
