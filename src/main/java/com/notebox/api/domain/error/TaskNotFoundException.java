package com.notebox.api.domain.error;

/** The requested task does not exist in the caller's tenant (C-01). */
public class TaskNotFoundException extends DomainException {

    public TaskNotFoundException() {
        super(ErrorCategory.NOT_FOUND, "task.not_found");
    }
}
