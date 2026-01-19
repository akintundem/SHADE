package eventplanner.common.exception.exceptions;

/**
 * Exception for HTTP 401 Unauthorized status.
 * Used when authentication is required but missing or invalid.
 */
public class UnauthorizedException extends ApiException {
    
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED, message, cause);
    }
}
