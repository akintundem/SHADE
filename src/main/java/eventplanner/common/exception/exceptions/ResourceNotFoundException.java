package eventplanner.common.exception.exceptions;

/**
 * Exception for HTTP 404 Not Found status.
 * Used when a requested resource cannot be found.
 */
public class ResourceNotFoundException extends ApiException {
    
    public ResourceNotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(ErrorCode.NOT_FOUND, message, cause);
    }
}
