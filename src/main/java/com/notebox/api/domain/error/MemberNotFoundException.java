package com.notebox.api.domain.error;

/** No such member in the caller's tenant. A foreign tenant's member is indistinguishable (BR-01). */
public class MemberNotFoundException extends DomainException {

    public MemberNotFoundException() {
        super(ErrorCategory.NOT_FOUND, "member.not_found");
    }
}
