package com.notebox.api.domain;

import java.util.UUID;

/** Marks an entity as owned by a tenant (BR-02). Every such entity is reached only via a tenant-scoped repository (AD-03). */
public interface TenantOwned {

    UUID getTenantId();
}
