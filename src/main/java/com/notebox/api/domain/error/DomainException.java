package com.notebox.api.domain.error;

/**
 * Base for the domain's specific error conditions (constitution §Errors). Carries a dot-namespaced
 * message key and an {@link ErrorCategory} — never an HTTP status, which is an api concern (AD-01).
 * Concrete subclasses name one condition each (e.g. {@code AnnotationTypeNotFoundException}).
 */
public abstract class DomainException extends RuntimeException {

    private final ErrorCategory category;
    private final String messageKey;
    private final transient Object[] args;

    protected DomainException(ErrorCategory category, String messageKey, Object... args) {
        super(messageKey);
        this.category = category;
        this.messageKey = messageKey;
        this.args = args;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
