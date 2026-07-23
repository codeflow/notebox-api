package com.notebox.api.domain.error;

/** Domain-level classification of an error; translated to an HTTP status only at the api edge (AD-01). */
public enum ErrorCategory {
    NOT_FOUND,
    CONFLICT,
    INVALID
}
