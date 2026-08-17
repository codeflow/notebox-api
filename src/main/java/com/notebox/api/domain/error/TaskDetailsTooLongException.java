package com.notebox.api.domain.error;

/**
 * A task's rich-text details exceed what the details column can store — measured on the sanitized
 * value, in UTF-8 bytes (FR-14; feat-005 precedent for text values).
 */
public class TaskDetailsTooLongException extends DomainException {

    public TaskDetailsTooLongException() {
        super(ErrorCategory.INVALID, "task.details.too_long");
    }
}
