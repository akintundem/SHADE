package eventplanner.common.exception.exceptions;

/**
 * Centralized error codes for API exceptions.
 * Each error code maps to a specific HTTP status and provides a consistent way
 * to identify error types across the application.
 */
public enum ErrorCode {
    // Ticket errors (400-409)
    TICKET_TYPE_SOLD_OUT("TICKET_TYPE_SOLD_OUT", 409),
    MAX_TICKETS_EXCEEDED("MAX_TICKETS_EXCEEDED", 400),
    TICKET_TYPE_NOT_AVAILABLE("TICKET_TYPE_NOT_AVAILABLE", 409),
    TICKET_CANCEL_SAVE_FAILED("TICKET_CANCEL_SAVE_FAILED", 500),
    
    // Checkout errors (400-410)
    CHECKOUT_CANCELLED("CHECKOUT_CANCELLED", 400),
    CHECKOUT_COMPLETED("CHECKOUT_COMPLETED", 409),
    CHECKOUT_INACTIVE("CHECKOUT_INACTIVE", 400),
    CHECKOUT_EXPIRED("CHECKOUT_EXPIRED", 410),
    
    // Generic HTTP errors
    BAD_REQUEST("BAD_REQUEST", 400),
    UNAUTHORIZED("UNAUTHORIZED", 401),
    FORBIDDEN("FORBIDDEN", 403),
    NOT_FOUND("NOT_FOUND", 404),
    CONFLICT("CONFLICT", 409),
    INTERNAL_ERROR("INTERNAL_ERROR", 500);
    
    private final String code;
    private final int httpStatus;
    
    ErrorCode(String code, int httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }
    
    public String getCode() {
        return code;
    }
    
    public int getHttpStatus() {
        return httpStatus;
    }
    
    /**
     * Find ErrorCode by code string
     */
    public static ErrorCode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return null;
    }
}
