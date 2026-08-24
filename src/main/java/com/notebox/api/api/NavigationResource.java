package com.notebox.api.api;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.notebox.api.api.dto.NavigationTreeDto;
import com.notebox.api.application.navigation.NavigationService;

import io.quarkus.security.Authenticated;

/**
 * The Navigator's data (FR-09, C28). One read returns both roots for the caller's tenant; a node is
 * resolved by calling the matching listing with its group filter (C30), so the tree itself never
 * enumerates a record or a task (OQ-23).
 */
@Path("/navigation")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class NavigationResource {

    private final NavigationService service;

    public NavigationResource(NavigationService service) {
        this.service = service;
    }

    @GET
    @Transactional
    public NavigationTreeDto tree() {
        return service.tree();
    }
}
