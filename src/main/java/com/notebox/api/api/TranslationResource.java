package com.notebox.api.api;

import java.util.List;
import java.util.Locale;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.notebox.api.api.dto.TranslationDto;
import com.notebox.api.api.dto.TranslationInput;
import com.notebox.api.application.i18n.TranslationCatalog;

/**
 * Runtime translation management (FR-16, design 17).
 *
 * <p>Administrator-only (C-03): wording is what every member of the workspace reads, so changing it
 * is an administrative act, and each change is audited (C-10).
 */
@Path("/translations")
@RolesAllowed("ADMIN")
public class TranslationResource {

    private final TranslationCatalog catalog;

    public TranslationResource(TranslationCatalog catalog) {
        this.catalog = catalog;
    }

    /** The whole catalog for one locale: the product's wording and this tenant's, side by side. */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<TranslationDto> list(@QueryParam("locale") @DefaultValue("en") String locale) {
        return catalog.list(Locale.forLanguageTag(locale));
    }

    /** Sets this tenant's wording for one key. An unknown key is refused (404), not created. */
    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TranslationDto set(
            @PathParam("key") String key,
            @QueryParam("locale") @DefaultValue("en") String locale,
            @Valid TranslationInput input) {
        return catalog.set(Locale.forLanguageTag(locale), key, input.value());
    }

    /** Drops this tenant's wording so the key falls back to the product's own. */
    @DELETE
    @Path("/{key}")
    public void clear(
            @PathParam("key") String key, @QueryParam("locale") @DefaultValue("en") String locale) {
        catalog.clear(Locale.forLanguageTag(locale), key);
    }
}
