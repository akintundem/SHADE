package eventplanner.common.exception;

/**
 * Exception for HTTP 429 Too Many Requests status.
 * Used when the user has sent too many requests in a given amount of time (rate limiting).
 */
public class TooManyRequestsException extends ApiException {
    public TooManyRequestsException(String message) {
        super(null, message, 429);
    }
}

