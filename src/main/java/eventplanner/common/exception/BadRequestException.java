package eventplanner.common.exception;

/**
 * Exception for HTTP 400 Bad Request status.
 * Used when the request is malformed or contains invalid data.
 */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(null, message, 400);
    }
    
    @Deprecated
    public BadRequestException(String code, String message) {
        super(null, message, 400);
    }
}


