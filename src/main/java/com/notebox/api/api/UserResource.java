package com.notebox.api.api;

import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.notebox.api.api.dto.UserDto;
import com.notebox.api.api.dto.UserPatch;
import com.notebox.api.api.error.ApiException;
import com.notebox.api.domain.User;
import com.notebox.api.infrastructure.persistence.UserRepository;

import io.quarkus.security.Authenticated;

/**
 * Tenant-scoped access to users. A user in another tenant is indistinguishable from a missing one
 * (404), never disclosing existence (BR-01).
 */
@Path("/users")
@Authenticated
public class UserResource {

    private final UserRepository userRepository;

    public UserResource(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GET
    @Path("/{id}")
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    public UserDto getById(@PathParam("id") UUID id) {
        return userRepository.findByIdInTenant(id)
                .map(UserDto::from)
                .orElseThrow(ApiException::notFound);
    }

    @PATCH
    @Path("/{id}")
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public UserDto update(@PathParam("id") UUID id, @Valid UserPatch patch) {
        User user = userRepository.findByIdInTenant(id).orElseThrow(ApiException::notFound);
        if (patch.displayName() != null) {
            user.setDisplayName(patch.displayName());
        }
        if (patch.locale() != null) {
            user.setLocalePreference(patch.locale());
        }
        return UserDto.from(user);
    }
}
