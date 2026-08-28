package com.notebox.api.api;

import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.notebox.api.api.dto.MemberActive;
import com.notebox.api.api.dto.MemberCreate;
import com.notebox.api.api.dto.PasswordReset;
import com.notebox.api.api.dto.UserDto;
import com.notebox.api.api.dto.UserPatch;
import com.notebox.api.api.error.ApiException;
import com.notebox.api.application.member.MemberService;
import com.notebox.api.domain.Role;
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
    private final MemberService members;

    public UserResource(UserRepository userRepository, MemberService members) {
        this.userRepository = userRepository;
        this.members = members;
    }

    /**
     * Every member of the caller's tenant.
     *
     * <p>Administrator-only (C-03): the member list is who can reach the workspace, which is not
     * ordinary member data. This is the first endpoint in the app to carry a role check.
     */
    @GET
    @RolesAllowed("ADMIN")
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserDto> list() {
        return members.list().stream().map(UserDto::from).toList();
    }

    /** Provisions a member in the caller's own tenant (OQ-11). Administrator-only. */
    @POST
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public UserDto create(@Valid MemberCreate input) {
        Role role = input.role() == null ? Role.MEMBER : Role.valueOf(input.role());
        return UserDto.from(
                members.create(input.email(), input.displayName(), role, input.password()));
    }

    /**
     * Sets a member's password on their behalf — the administered answer to "Forgot password?",
     * since this product has no self-service reset. Administrator-only, and audited.
     */
    @PUT
    @Path("/{id}/password")
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.APPLICATION_JSON)
    public void resetPassword(@PathParam("id") UUID id, @Valid PasswordReset input) {
        members.resetPassword(id, input.password());
    }

    /** Activates or deactivates a member. Administrator-only, and audited. */
    @PUT
    @Path("/{id}/active")
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public UserDto setActive(@PathParam("id") UUID id, MemberActive input) {
        return UserDto.from(members.setActive(id, input.active()));
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
