package com.notebox.api.domain.error;

/** An uploaded image exceeds the maximum allowed size (NFR-04, C-07). */
public class ImageTooLargeException extends DomainException {

    public ImageTooLargeException() {
        super(ErrorCategory.INVALID, "annotation.image.too_large");
    }
}
