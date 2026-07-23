package com.notebox.api.domain.error;

/** The requested image does not exist in the caller's tenant (C-01). */
public class ImageNotFoundException extends DomainException {

    public ImageNotFoundException() {
        super(ErrorCategory.NOT_FOUND, "annotation.image.not_found");
    }
}
