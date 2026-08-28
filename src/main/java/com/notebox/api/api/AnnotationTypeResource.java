package com.notebox.api.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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

import com.notebox.api.api.dto.AnnotationTypeDto;
import com.notebox.api.api.dto.AnnotationTypeInput;
import com.notebox.api.api.dto.AnnotationTypeListItemDto;
import com.notebox.api.application.annotation.AnnotationTypeService;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.infrastructure.persistence.AnnotationRecordRepository;

import io.quarkus.security.Authenticated;

/**
 * Annotation-type CRUD (FR-01/02/03). Every endpoint is authenticated and tenant-scoped: a foreign
 * tenant's id yields 404 and discloses nothing (C-01, C-02). Input is validated at the edge (AD-07).
 */
@Path("/annotation-types")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class AnnotationTypeResource {

    private final AnnotationTypeService service;
    private final AnnotationRecordRepository records;

    public AnnotationTypeResource(AnnotationTypeService service, AnnotationRecordRepository records) {
        this.service = service;
        this.records = records;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response create(@Valid AnnotationTypeInput input) {
        AnnotationTypeDto created = AnnotationTypeDto.from(service.create(input));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    @Transactional
    public List<AnnotationTypeListItemDto> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("50") int size) {
        List<AnnotationType> types = service.list(page, size);
        // One grouped count for the whole tenant, not one query per row.
        Map<UUID, Long> counts = records.recordCountsByTypeInTenant().stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
        return types.stream()
                .map(type -> AnnotationTypeListItemDto.from(type, counts.getOrDefault(type.getId(), 0L)))
                .toList();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public AnnotationTypeDto get(@PathParam("id") UUID id) {
        return AnnotationTypeDto.from(service.get(id));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public AnnotationTypeDto replace(@PathParam("id") UUID id, @Valid AnnotationTypeInput input) {
        return AnnotationTypeDto.from(service.replace(id, input));
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") UUID id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
