package com.notebox.api.api;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.notebox.api.api.dto.MeDto;
import com.notebox.api.api.error.ApiException;
import com.notebox.api.infrastructure.persistence.TenantRepository;
import com.notebox.api.infrastructure.persistence.UserRepository;
import com.notebox.api.infrastructure.security.TenantContext;

import io.quarkus.security.Authenticated;

/** Returns the authenticated caller's own identity, scoped to their tenant. */
@Path("/me")
@Authenticated
public class MeResource {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantContext tenantContext;

    public MeResource(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            TenantContext tenantContext) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.tenantContext = tenantContext;
    }

    @GET
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    public MeDto me() {
        // The tenant id comes from the token, never from the request — that is what keeps this
        // lookup from becoming a way to read another tenant's name (BR-01, AD-03).
        var tenant = tenantRepository.findById(tenantContext.tenantId())
                .orElseThrow(ApiException::notFound);
        return userRepository.findByIdInTenant(tenantContext.userId())
                .map(user -> MeDto.from(user, tenant))
                .orElseThrow(ApiException::notFound);
    }
}
