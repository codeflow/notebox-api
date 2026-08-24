package com.notebox.api.domain.error;

/**
 * An item was pointed at a group of the other domain — a task at an annotation group, or the
 * reverse. The namespaces are separate (OQ-04), and no foreign key can express the rule.
 */
public class GroupDomainMismatchException extends DomainException {

    public GroupDomainMismatchException() {
        super(ErrorCategory.CONFLICT, "group.domain.mismatch");
    }
}
