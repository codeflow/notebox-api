package com.notebox.api.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.notebox.api.api.dto.GroupDto;
import com.notebox.api.api.dto.GroupInput;
import com.notebox.api.api.dto.PageDto;
import com.notebox.api.api.validation.ValidGroupDomain;
import com.notebox.api.application.group.GroupAggregates;
import com.notebox.api.application.group.GroupService;
import com.notebox.api.domain.Group;
import com.notebox.api.domain.GroupDomain;

import io.quarkus.security.Authenticated;

/**
 * Group endpoints (FR-08). Every endpoint is authenticated and tenant-scoped: a foreign tenant's id
 * yields 404 and discloses nothing (C-01, C-02). The listing is per-namespace — {@code domain} is
 * required, because annotation groups and task groups are separate namespaces (OQ-04) and a mixed
 * listing would be meaningless to either consumer.
 */
@Path("/groups")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class GroupResource {

    private final GroupService service;

    public GroupResource(GroupService service) {
        this.service = service;
    }

    /** One page of the tenant's groups in one namespace, name ascending (NFR-08, OQ-25). */
    @GET
    @Transactional
    public PageDto<GroupDto> list(
            @QueryParam("domain")
            @NotNull(message = "group.domain.required")
            @ValidGroupDomain String domain,
            @QueryParam("page") @DefaultValue("0")
            @Min(value = 0, message = "group.list.size.out_of_bounds") int page,
            @QueryParam("size") @DefaultValue("50")
            @Min(value = 1, message = "group.list.size.out_of_bounds")
            @Max(value = 200, message = "group.list.size.out_of_bounds") int size) {
        GroupDomain parsed = GroupDomain.valueOf(domain);
        List<Group> groups = service.list(parsed, page, size);
        Map<UUID, GroupAggregates> aggregates = service.aggregatesFor(parsed, groups);
        List<GroupDto> items = groups.stream()
                .map(group -> GroupDto.from(group, aggregates.get(group.getId())))
                .toList();
        return new PageDto<>(items, page, size, service.count(parsed));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response create(@Valid GroupInput input) {
        Group group = service.create(input);
        return Response.status(Response.Status.CREATED)
                .entity(GroupDto.from(group, service.aggregatesOf(group)))
                .build();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public GroupDto get(@PathParam("id") UUID id) {
        Group group = service.get(id);
        return GroupDto.from(group, service.aggregatesOf(group));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public GroupDto replace(@PathParam("id") UUID id, @Valid GroupInput input) {
        Group group = service.replace(id, input);
        return GroupDto.from(group, service.aggregatesOf(group));
    }

    /** Deletes the group only — its members survive and become ungrouped (OQ-24), audited (C-10). */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") UUID id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
