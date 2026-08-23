package com.notebox.api.domain.error;

/**
 * The caller's tenant already has a group with the requested name in that domain. Uniqueness is per
 * tenant AND per domain, so the same name may still be free in the other namespace (OQ-04).
 */
public class GroupNameTakenException extends DomainException {

    public GroupNameTakenException() {
        super(ErrorCategory.CONFLICT, "group.name.duplicate");
    }
}
