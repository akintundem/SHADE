package ai.eventplanner.common.exception;

public class ForbiddenException extends ApiException {
    public ForbiddenException(String code, String message) {
        super(code, message, 403);
    }
}

package ai.eventplanner.common.exception;

public class ForbiddenException extends RuntimeException {
    
    public ForbiddenException(String message) {
        super(message);
    }
    
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
