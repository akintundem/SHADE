package eventplanner.common.exception.exceptions;

/**
 * Exception for HTTP 400 Bad Request status.
 * Used when the request is malformed or contains invalid data.
 */
public class BadRequestException extends ApiException {
    
    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(ErrorCode.BAD_REQUEST, message, cause);
    }
}
