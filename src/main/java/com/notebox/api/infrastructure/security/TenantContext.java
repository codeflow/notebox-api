package com.notebox.api.infrastructure.security;

import java.util.UUID;

import com.notebox.api.domain.Role;

/**
 * The caller's tenant and identity for the current request, derived solely from the verified JWT
 * (never from request input). It is the single source consumers use to scope data access (AD-03, BR-01).
 */
public interface TenantContext {

    /** The caller's tenant; never null on an authenticated request. */
    UUID tenantId();

    /** The caller's user id. */
    UUID userId();

    /** The caller's role. */
    Role role();

    /** True when the current request carried a valid token. */
    boolean authenticated();
}
