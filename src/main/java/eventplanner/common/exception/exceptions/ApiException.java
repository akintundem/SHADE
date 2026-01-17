package eventplanner.common.exception.exceptions;

/**
 * Base exception for all API-related errors.
 * Supports error codes, HTTP status codes, and exception chaining.
 */
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final int status;

    /**
     * Create an ApiException with an error code
     */
    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.status = errorCode != null ? errorCode.getHttpStatus() : 500;
    }

    /**
     * Create an ApiException with an error code and cause
     */
    public ApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = errorCode != null ? errorCode.getHttpStatus() : 500;
    }

    /**
     * Create an ApiException with explicit status (for backward compatibility)
     * @deprecated Use ErrorCode enum instead
     */
    @Deprecated
    public ApiException(String code, String message, int status) {
        super(message);
        this.errorCode = ErrorCode.fromCode(code);
        this.status = status;
    }

    /**
     * Get the error code enum
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Get the error code string (for backward compatibility)
     */
    public String getCode() {
        return errorCode != null ? errorCode.getCode() : null;
    }

    /**
     * Get the HTTP status code
     */
    public int getStatus() {
        return status;
    }
}
