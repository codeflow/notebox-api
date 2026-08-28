package com.notebox.api.domain.error;

/** The workspace address is already in use. Slugs are global — they identify the organization. */
public class OrganizationSlugTakenException extends DomainException {

    public OrganizationSlugTakenException() {
        super(ErrorCategory.CONFLICT, "organization.slug.duplicate");
    }
}
