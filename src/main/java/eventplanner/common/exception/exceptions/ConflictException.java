package eventplanner.common.exception.exceptions;

/**
 * Exception for HTTP 409 Conflict status.
 * Used when a request conflicts with the current state of the resource.
 * Examples: duplicate email, resource already exists, optimistic locking conflicts.
 */
public class ConflictException extends ApiException {
    
    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(ErrorCode.CONFLICT, message, cause);
    }
}
