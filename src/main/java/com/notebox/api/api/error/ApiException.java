package com.notebox.api.api.error;

/**
 * Application error carrying a stable machine {@code code} and an HTTP status. The user-facing message
 * is resolved from the code by the exception mapper (never a raw literal — AD-05, BR-08).
 */
public class ApiException extends RuntimeException {

    private final String code;
    private final int status;

    public ApiException(String code, int status) {
        super(code);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }

    public static ApiException invalidCredentials() {
        return new ApiException("AUTH_INVALID_CREDENTIALS", 401);
    }

    public static ApiException notFound() {
        return new ApiException("RESOURCE_NOT_FOUND", 404);
    }
}
