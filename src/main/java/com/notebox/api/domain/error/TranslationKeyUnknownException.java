package com.notebox.api.domain.error;

/**
 * The product defines no such message key. A tenant rewords what the product says; it does not
 * invent new messages, so an unknown key is refused rather than quietly created (FR-16).
 */
public class TranslationKeyUnknownException extends DomainException {

    public TranslationKeyUnknownException() {
        super(ErrorCategory.NOT_FOUND, "translation.key.unknown");
    }
}
