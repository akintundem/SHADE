package eventplanner.common.exception.exceptions;

/**
 * Exception for HTTP 403 Forbidden status.
 * Used when the user is authenticated but doesn't have permission to access the resource.
 */
public class ForbiddenException extends ApiException {
    
    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
    
    public ForbiddenException(String message, Throwable cause) {
        super(ErrorCode.FORBIDDEN, message, cause);
    }
}
