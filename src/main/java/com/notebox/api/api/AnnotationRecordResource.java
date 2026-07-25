package com.notebox.api.api;

import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.notebox.api.api.dto.AnnotationRecordDto;
import com.notebox.api.api.dto.AnnotationRecordInput;
import com.notebox.api.api.dto.RevealResponse;
import com.notebox.api.application.annotation.AnnotationRecordService;
import com.notebox.api.application.annotation.AnnotationTypeService;
import com.notebox.api.domain.AnnotationRecord;
import com.notebox.api.domain.AnnotationType;

import io.quarkus.security.Authenticated;

/**
 * Annotation-record CRUD + secret reveal (FR-04, FR-06, FR-18). Every endpoint is authenticated and
 * tenant-scoped: a foreign tenant's id yields 404 and discloses nothing (C-01, C-02). Reveal is
 * gated inside the service to the elevated role and returns 403 with a localized key (C-03).
 */
@Path("/annotation-records")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class AnnotationRecordResource {

    private final AnnotationRecordService service;
    private final AnnotationTypeService typeService;

    public AnnotationRecordResource(AnnotationRecordService service, AnnotationTypeService typeService) {
        this.service = service;
        this.typeService = typeService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response create(@Valid AnnotationRecordInput input) {
        AnnotationRecord record = service.create(input);
        return Response.status(Response.Status.CREATED).entity(toDto(record)).build();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public AnnotationRecordDto get(@PathParam("id") UUID id) {
        return toDto(service.get(id));
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public AnnotationRecordDto replace(@PathParam("id") UUID id, @Valid AnnotationRecordInput input) {
        return toDto(service.update(id, input));
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") UUID id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/values/{fieldId}/reveal")
    @Transactional
    public RevealResponse reveal(@PathParam("id") UUID id, @PathParam("fieldId") UUID fieldId) {
        String cleartext = service.reveal(id, fieldId);
        return new RevealResponse(id, fieldId, cleartext);
    }

    private AnnotationRecordDto toDto(AnnotationRecord record) {
        AnnotationType type = typeService.get(record.getAnnotationTypeId());
        return AnnotationRecordDto.from(record, type);
    }
}
