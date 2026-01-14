package eventplanner.common.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.Problem;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.HibernateException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.DataAccessException;
import java.sql.SQLException;
import org.springframework.transaction.TransactionSystemException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler using Zalando Problem (RFC 7807).
 * Uses ProblemBuilder from common exception library to create standardized error responses.
 * 
 * STRICT SECURITY POLICY: NEVER expose internal details to clients.
 * All error messages are sanitized. Internal details are never leaked.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Problem> handleValidationExceptions(
            MethodArgumentNotValidException ex, NativeWebRequest request) {

        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Problem problem = ProblemBuilder.validationError(errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Problem> handleIllegalArgumentException(
            IllegalArgumentException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.badRequest("Invalid request parameters");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Problem> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.badRequest("Malformed request payload");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Problem> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.badRequest("Required parameter is missing");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Problem> handleResourceNotFoundException(
            ResourceNotFoundException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.notFound("The requested resource was not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Problem> handleUnauthorizedException(
            UnauthorizedException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.unauthorized("Authentication required");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Problem> handleAccessDenied(
            AccessDeniedException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.forbidden("Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Problem> handleForbiddenException(
            ForbiddenException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.forbidden("Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Problem> handleApiException(
            ApiException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.fromStatus(ex.getStatus(), getGenericMessageForStatus(ex.getStatus()));
        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Problem> handleNoHandlerFoundException(
            NoHandlerFoundException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.notFound("The requested resource was not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Problem> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.methodNotAllowed("The request method is not supported for this resource");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problem);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Problem> handleResponseStatusException(
            ResponseStatusException ex, NativeWebRequest request) {

        int statusCode = ex.getStatusCode().value();
        Problem problem = ProblemBuilder.fromStatus(statusCode, getGenericMessageForStatus(statusCode));
        return ResponseEntity.status(statusCode).body(problem);
    }

    // ============================================================================
    // HIBERNATE / JPA / PERSISTENCE EXCEPTION HANDLERS
    // ============================================================================

    @ExceptionHandler(StaleObjectStateException.class)
    public ResponseEntity<Problem> handleStaleObjectStateException(
            StaleObjectStateException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.conflict("The resource has been modified. Please refresh and try again.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(StaleStateException.class)
    public ResponseEntity<Problem> handleStaleStateException(
            StaleStateException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.conflict("The resource has been modified. Please refresh and try again.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(HibernateException.class)
    public ResponseEntity<Problem> handleHibernateException(
            HibernateException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.internalError("A database error occurred. Please try again.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<Problem> handlePersistenceException(
            PersistenceException ex, NativeWebRequest request) {

        // Check for optimistic locking
        Throwable cause = ex.getCause();
        if (cause != null && (cause instanceof StaleObjectStateException ||
            cause instanceof StaleStateException ||
            (cause.getMessage() != null && cause.getMessage().contains("Row was updated or deleted")))) {

            Problem problem = ProblemBuilder.conflict("The resource has been modified. Please refresh and try again.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
        }

        Problem problem = ProblemBuilder.internalError("A database error occurred. Please try again.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<Problem> handleJpaOptimisticLockException(
            OptimisticLockException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.conflict("The resource has been modified. Please refresh and try again.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Problem> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.conflict("The resource has been modified. Please refresh and try again.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Problem> handleDataAccessException(
            DataAccessException ex, NativeWebRequest request) {

        String exceptionMessage = ex.getMessage();
        if (exceptionMessage != null && (exceptionMessage.contains("Row was updated or deleted") ||
            exceptionMessage.contains("StaleObjectStateException") ||
            exceptionMessage.contains("optimistic lock") ||
            exceptionMessage.contains("unsaved-value mapping"))) {

            Problem problem = ProblemBuilder.conflict("The resource has been modified. Please refresh and try again.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
        }

        Problem problem = ProblemBuilder.internalError("A database error occurred. Please try again.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Problem> handleSQLException(
            SQLException ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.internalError("A database error occurred. Please try again.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<Problem> handleTransactionSystemException(
            TransactionSystemException ex, NativeWebRequest request) {

        Throwable rootCause = ex.getRootCause();
        if (rootCause != null) {
            String rootMessage = rootCause.getMessage();
            if (rootMessage != null && (rootMessage.contains("Row was updated or deleted") ||
                rootMessage.contains("StaleObjectStateException") ||
                rootMessage.contains("optimistic lock"))) {

                Problem problem = ProblemBuilder.conflict("The resource has been modified. Please refresh and try again.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
            }
        }

        Problem problem = ProblemBuilder.internalError("A database error occurred. Please try again.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Problem> handleRuntimeException(
            RuntimeException ex, NativeWebRequest request) {

        String message = ex.getMessage();
        if (message != null && ResponseSanitizer.containsSensitiveInfo(message)) {
            Problem problem = ProblemBuilder.conflict("The resource has been modified. Please refresh and try again.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
        }

        Problem problem = ProblemBuilder.internalError("An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Problem> handleGenericException(
            Exception ex, NativeWebRequest request) {

        Problem problem = ProblemBuilder.internalError("An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private String getGenericMessageForStatus(int status) {
        return ProblemBuilder.getGenericMessageForStatus(status);
    }
}
