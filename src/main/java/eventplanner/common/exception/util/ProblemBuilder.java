package eventplanner.common.exception.util;

import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import java.net.URI;
import java.util.Map;

/**
 * Utility class for building Zalando Problem objects.
 * Centralizes Problem creation logic in the common exception library.
 */
public class ProblemBuilder {

    private static final URI DEFAULT_TYPE = URI.create("about:blank");

    /**
     * Create a Problem for validation errors
     */
    public static Problem validationError(Map<String, Object> errors) {
        return Problem.builder()
                .withType(DEFAULT_TYPE)
                .withTitle("Validation Failed")
                .withStatus(Status.BAD_REQUEST)
                .withDetail("Request validation failed")
                .with("errors", errors)
                .build();
    }

    /**
     * Create a Problem for bad request errors
     */
    public static Problem badRequest(String detail) {
        return Problem.builder()
                .withType(DEFAULT_TYPE)
                .withTitle("Bad Request")
                .withStatus(Status.BAD_REQUEST)
                .withDetail(detail != null ? detail : "Invalid request parameters")
                .build();
    }

    /**
     * Create a Problem for not found errors
     */
    public static Problem notFound(String detail) {
        return Problem.builder()
                .withType(DEFAULT_TYPE)
                .withTitle("Not Found")
                .withStatus(Status.NOT_FOUND)
                .withDetail(detail != null ? detail : "The requested resource was not found")
                .build();
    }

    /**
     * Create a Problem for unauthorized errors
     */
    public static Problem unauthorized(String detail) {
        return Problem.builder()
                .withType(DEFAULT_TYPE)
                .withTitle("Unauthorized")
                .withStatus(Status.UNAUTHORIZED)
                .withDetail(detail != null ? detail : "Authentication required")
                .build();
    }

    /**
     * Create a Problem for forbidden errors
     */
    public static Problem forbidden(String detail) {
        return Problem.builder()
                .withType(DEFAULT_TYPE)
                .withTitle("Forbidden")
                .withStatus(Status.FORBIDDEN)
                .withDetail(detail != null ? detail : "Access denied")
                .build();
    }

    /**
     * Create a Problem for conflict errors
     */
    public static Problem conflict(String detail) {
        return Problem.builder()
                .withType(DEFAULT_TYPE)
                .withTitle("Conflict")
                .withStatus(Status.CONFLICT)
                .withDetail(detail != null ? detail : "The resource has been modified. Please refresh and try again.")
                .build();
    }

    /**
     * Create a Problem for internal server errors
     */
    public static Problem internalError(String detail) {
        return Problem.builder()
                .withType(DEFAULT_TYPE)
                .withTitle("Internal Server Error")
                .withStatus(Status.INTERNAL_SERVER_ERROR)
                .withDetail(detail != null ? detail : "An unexpected error occurred")
                .build();
    }

    /**
     * Create a Problem for method not allowed errors
     */
    public static Problem methodNotAllowed(String detail) {
        return Problem.builder()
                .withType(DEFAULT_TYPE)
                .withTitle("Method Not Allowed")
                .withStatus(Status.METHOD_NOT_ALLOWED)
                .withDetail(detail != null ? detail : "The request method is not supported for this resource")
                .build();
    }

    /**
     * Create a Problem from HTTP status code
     */
    public static Problem fromStatus(int statusCode, String detail) {
        Status status = toZalandoStatus(statusCode);
        String title = status.getReasonPhrase();
        String message = detail != null ? detail : getGenericMessageForStatus(statusCode);

        return Problem.builder()
                .withType(DEFAULT_TYPE)
                .withTitle(title)
                .withStatus(status)
                .withDetail(message)
                .build();
    }

    /**
     * Create a Problem from ApiException with error code
     */
    public static Problem fromApiException(eventplanner.common.exception.exceptions.ApiException ex) {
        Status status = toZalandoStatus(ex.getStatus());
        String message = ex.getMessage() != null ? ex.getMessage() : getGenericMessageForStatus(ex.getStatus());
        
        var builder = Problem.builder()
                .withType(DEFAULT_TYPE)
                .withTitle(status.getReasonPhrase())
                .withStatus(status)
                .withDetail(message);
        
        // Include error code if available
        if (ex.getErrorCode() != null) {
            builder.with("code", ex.getErrorCode().getCode());
        } else if (ex.getCode() != null) {
            builder.with("code", ex.getCode());
        }
        
        return builder.build();
    }

    /**
     * Create a Problem from HTTP status code with custom title
     */
    public static Problem fromStatus(int statusCode, String title, String detail) {
        Status status = toZalandoStatus(statusCode);
        String message = detail != null ? detail : getGenericMessageForStatus(statusCode);

        return Problem.builder()
                .withType(DEFAULT_TYPE)
                .withTitle(title != null ? title : status.getReasonPhrase())
                .withStatus(status)
                .withDetail(message)
                .build();
    }

    /**
     * Convert HTTP status code to Zalando Status
     */
    public static Status toZalandoStatus(int httpStatus) {
        return switch (httpStatus) {
            case 400 -> Status.BAD_REQUEST;
            case 401 -> Status.UNAUTHORIZED;
            case 403 -> Status.FORBIDDEN;
            case 404 -> Status.NOT_FOUND;
            case 405 -> Status.METHOD_NOT_ALLOWED;
            case 409 -> Status.CONFLICT;
            case 429 -> Status.TOO_MANY_REQUESTS;
            case 500 -> Status.INTERNAL_SERVER_ERROR;
            case 502 -> Status.BAD_GATEWAY;
            case 503 -> Status.SERVICE_UNAVAILABLE;
            case 504 -> Status.GATEWAY_TIMEOUT;
            default -> Status.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * Get generic message for HTTP status code
     */
    public static String getGenericMessageForStatus(int status) {
        return switch (status) {
            case 400 -> "Invalid request";
            case 401 -> "Authentication required";
            case 403 -> "Access denied";
            case 404 -> "The requested resource was not found";
            case 405 -> "The request method is not supported";
            case 409 -> "A conflict occurred with the current state of the resource";
            case 429 -> "Too many requests. Please try again later";
            case 500 -> "An unexpected error occurred";
            default -> "An error occurred";
        };
    }
}
