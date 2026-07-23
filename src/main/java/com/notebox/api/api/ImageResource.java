package com.notebox.api.api;

import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.notebox.api.api.dto.ImageRefDto;
import com.notebox.api.application.annotation.ImageService;
import com.notebox.api.domain.Image;

import io.quarkus.security.Authenticated;

/**
 * Binary icon storage (AD-04, FR-07). Upload is a raw image body whose {@code Content-Type} names the
 * format (no multipart); the bytes are streamed back from the dedicated download endpoint. Both are
 * authenticated and tenant-scoped (C-01, C-02).
 */
@Path("/images")
@Authenticated
public class ImageResource {

    private final ImageService images;

    public ImageResource(ImageService images) {
        this.images = images;
    }

    @POST
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response upload(byte[] body, @Context HttpHeaders headers) {
        Image stored = images.store(contentType(headers), body);
        return Response.status(Response.Status.CREATED).entity(ImageRefDto.from(stored)).build();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public Response download(@PathParam("id") UUID id) {
        Image image = images.get(id);
        return Response.ok(image.getBytes()).type(image.getContentType()).build();
    }

    private String contentType(HttpHeaders headers) {
        MediaType mediaType = headers.getMediaType();
        return mediaType == null ? null : mediaType.getType() + "/" + mediaType.getSubtype();
    }
}
