package com.notebox.api.api;

import java.util.List;
import java.util.UUID;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import com.notebox.api.api.dto.PageDto;
import com.notebox.api.api.dto.SubtaskInput;
import com.notebox.api.api.dto.TaskDto;
import com.notebox.api.api.dto.TaskInput;
import com.notebox.api.api.dto.TaskListItemDto;
import com.notebox.api.application.content.RichTextSanitizer;
import com.notebox.api.application.group.GroupFilter;
import com.notebox.api.application.task.TaskService;

import io.quarkus.security.Authenticated;

/**
 * Task and subtask endpoints (FR-10..FR-14). Every endpoint is authenticated and tenant-scoped: a
 * foreign tenant's id yields 404 and discloses nothing (C-01, C-02). Subtask mutations return the
 * parent task so the client reads the recomputed status (BR-06) and dates (BR-07) without a second
 * round trip; the listing is paginated newest-first (NFR-08, OQ-21). Task details are re-sanitized
 * at the DTO boundary on every read (C-08 output half).
 */
@Path("/tasks")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class TaskResource {

    private final TaskService service;
    private final RichTextSanitizer sanitizer;

    public TaskResource(TaskService service, RichTextSanitizer sanitizer) {
        this.service = service;
        this.sanitizer = sanitizer;
    }

    /** One page of the tenant's tasks, newest first — createdAt desc, id desc (NFR-08, OQ-21). */
    @GET
    @Transactional
    public PageDto<TaskListItemDto> list(
            @QueryParam("group") String group,
            @QueryParam("page") @DefaultValue("0")
            @Min(value = 0, message = "task.list.size.out_of_bounds") int page,
            @QueryParam("size") @DefaultValue("50")
            @Min(value = 1, message = "task.list.size.out_of_bounds")
            @Max(value = 200, message = "task.list.size.out_of_bounds") int size) {
        GroupFilter filter = GroupFilter.parse(group);
        List<TaskListItemDto> items = service.list(filter, page, size).stream()
                .map(TaskListItemDto::from)
                .toList();
        return new PageDto<>(items, page, size, service.count(filter));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response create(@Valid TaskInput input) {
        return Response.status(Response.Status.CREATED)
                .entity(TaskDto.from(service.create(input), sanitizer))
                .build();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public TaskDto get(@PathParam("id") UUID id) {
        return TaskDto.from(service.get(id), sanitizer);
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public TaskDto update(@PathParam("id") UUID id, @Valid TaskInput input) {
        return TaskDto.from(service.update(id, input), sanitizer);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") UUID id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/subtasks")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response addSubtask(@PathParam("id") UUID id, @Valid SubtaskInput input) {
        return Response.status(Response.Status.CREATED)
                .entity(TaskDto.from(service.addSubtask(id, input), sanitizer))
                .build();
    }

    @PUT
    @Path("/{id}/subtasks/{subtaskId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public TaskDto updateSubtask(
            @PathParam("id") UUID id,
            @PathParam("subtaskId") UUID subtaskId,
            @Valid SubtaskInput input) {
        return TaskDto.from(service.updateSubtask(id, subtaskId, input), sanitizer);
    }

    @DELETE
    @Path("/{id}/subtasks/{subtaskId}")
    @Transactional
    public TaskDto removeSubtask(@PathParam("id") UUID id, @PathParam("subtaskId") UUID subtaskId) {
        return TaskDto.from(service.removeSubtask(id, subtaskId), sanitizer);
    }
}
