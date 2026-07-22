package com.notebox.api.api;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.notebox.api.api.dto.MeDto;
import com.notebox.api.api.error.ApiException;
import com.notebox.api.infrastructure.persistence.UserRepository;
import com.notebox.api.infrastructure.security.TenantContext;

import io.quarkus.security.Authenticated;

/** Returns the authenticated caller's own identity, scoped to their tenant. */
@Path("/me")
@Authenticated
public class MeResource {

    private final UserRepository userRepository;
    private final TenantContext tenantContext;

    public MeResource(UserRepository userRepository, TenantContext tenantContext) {
        this.userRepository = userRepository;
        this.tenantContext = tenantContext;
    }

    @GET
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    public MeDto me() {
        return userRepository.findByIdInTenant(tenantContext.userId())
                .map(MeDto::from)
                .orElseThrow(ApiException::notFound);
    }
}
