package com.notebox.api.domain.error;

/**
 * An update stated a domain other than the group's own. The domain is fixed at creation — changing
 * it would silently orphan every member — so the attempt is rejected rather than ignored.
 */
public class GroupDomainNotModifiableException extends DomainException {

    public GroupDomainNotModifiableException() {
        super(ErrorCategory.CONFLICT, "group.domain.not_modifiable");
    }
}
