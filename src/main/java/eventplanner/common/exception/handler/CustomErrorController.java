package eventplanner.common.exception.handler;

import eventplanner.common.exception.util.ProblemBuilder;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;

/**
 * Custom error controller using Zalando Problem (RFC 7807).
 * Uses ProblemBuilder from common exception library to create standardized error responses.
 * Catches errors from filters, security layer, and any other non-controller contexts.
 * 
 * STRICT SECURITY POLICY: Always return sanitized, generic error messages.
 */
@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Problem> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorMessage = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Throwable exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        // Determine appropriate status code
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        if (statusCode != null) {
            try {
                httpStatus = HttpStatus.valueOf(statusCode);
            } catch (IllegalArgumentException e) {
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }

        // Check if this is a persistence/database related error
        if (exception != null) {
            String exceptionMessage = exception.getMessage();
            if (exceptionMessage != null && isPersistenceError(exceptionMessage)) {
                httpStatus = HttpStatus.CONFLICT;
            }
        }

        // Get sanitized message
        String sanitizedMessage = getSanitizedMessage(httpStatus, errorMessage, exception);
        Problem problem = ProblemBuilder.fromStatus(httpStatus.value(), sanitizedMessage);

        return ResponseEntity.status(httpStatus).body(problem);
    }

    private boolean isPersistenceError(String message) {
        if (message == null) return false;
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("row was updated or deleted") ||
               lowerMessage.contains("staleobjectstate") ||
               lowerMessage.contains("stalestate") ||
               lowerMessage.contains("optimistic lock") ||
               lowerMessage.contains("hibernate") ||
               lowerMessage.contains("transaction") ||
               lowerMessage.contains("entity") ||
               lowerMessage.contains("persistence") ||
               lowerMessage.contains("jpa") ||
               message.contains("#") ||
               message.matches(".*[a-z]+\\.[a-z]+\\.[a-z]+\\.[A-Z][a-zA-Z]+.*");
    }

    private String getSanitizedMessage(HttpStatus status, String originalMessage, Throwable exception) {
        // Check if the error message contains internal details
        if (originalMessage != null && containsInternalDetails(originalMessage)) {
            return getGenericMessage(status);
        }

        // Check exception message
        if (exception != null && exception.getMessage() != null &&
            containsInternalDetails(exception.getMessage())) {
            return getGenericMessage(status);
        }

        // For 5xx errors, always use generic message
        if (status.is5xxServerError()) {
            return "An unexpected error occurred";
        }

        return getGenericMessage(status);
    }

    private boolean containsInternalDetails(String message) {
        if (message == null) return false;
        String lowerMessage = message.toLowerCase();

        return lowerMessage.contains("hibernate") ||
               lowerMessage.contains("entity") ||
               lowerMessage.contains("transaction") ||
               lowerMessage.contains("persistence") ||
               lowerMessage.contains("jpa") ||
               lowerMessage.contains("jdbc") ||
               lowerMessage.contains("sql") ||
               lowerMessage.contains("row was") ||
               lowerMessage.contains("stale") ||
               lowerMessage.contains("optimistic") ||
               lowerMessage.contains("unsaved-value") ||
               lowerMessage.contains("mapping was incorrect") ||
               lowerMessage.contains("bean") ||
               lowerMessage.contains("spring") ||
               lowerMessage.contains("repository") ||
               lowerMessage.contains("service") ||
               lowerMessage.contains("controller") ||
               lowerMessage.contains("stacktrace") ||
               lowerMessage.contains("exception") ||
               lowerMessage.contains("null pointer") ||
               lowerMessage.contains("class cast") ||
               message.contains("#") ||
               message.matches(".*[a-z]+\\.[a-z]+\\.[a-z]+\\.[A-Z][a-zA-Z]+.*") ||
               message.matches(".*[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}.*");
    }

    private String getGenericMessage(HttpStatus status) {
        return ProblemBuilder.getGenericMessageForStatus(status.value());
    }

}
