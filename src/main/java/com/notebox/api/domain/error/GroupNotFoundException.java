package com.notebox.api.domain.error;

/** The requested group does not exist in the caller's tenant (C-01). */
public class GroupNotFoundException extends DomainException {

    public GroupNotFoundException() {
        super(ErrorCategory.NOT_FOUND, "group.not_found");
    }
}
