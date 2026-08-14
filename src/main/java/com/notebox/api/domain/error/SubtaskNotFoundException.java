package com.notebox.api.domain.error;

/** The requested subtask does not exist on that task in the caller's tenant (C-01). */
public class SubtaskNotFoundException extends DomainException {

    public SubtaskNotFoundException() {
        super(ErrorCategory.NOT_FOUND, "task.subtask.not_found");
    }
}
