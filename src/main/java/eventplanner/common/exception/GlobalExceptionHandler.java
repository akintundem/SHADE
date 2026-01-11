package eventplanner.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Sanitizes error messages to prevent exposure of internal details.
     * Removes entity names, IDs, class paths, SQL statements, and other sensitive information.
     * Returns null if the message contains any internal details, forcing use of generic messages.
     */
    private String sanitizeMessage(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Reject messages containing any of these internal details - return null to force generic message
        if (lowerMessage.contains("row was") || 
            lowerMessage.contains("transaction") ||
            lowerMessage.contains("unsaved-value") ||
            lowerMessage.contains("mapping was incorrect") ||
            lowerMessage.contains("update ") ||
            lowerMessage.contains("select ") ||
            lowerMessage.contains("insert ") ||
            lowerMessage.contains("delete ") ||
            lowerMessage.contains("hibernate") ||
            lowerMessage.contains("entity") ||
            lowerMessage.contains("staleobject") ||
            lowerMessage.contains("optimistic") ||
            lowerMessage.contains("jpa") ||
            lowerMessage.contains("persistence") ||
            message.contains("#") || // Entity IDs like UserAccount#uuid
            message.matches(".*[a-z]+\\.[a-z]+\\.[a-z]+\\.[A-Z][a-zA-Z]+.*") || // Package paths
            message.matches(".*[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}.*")) { // UUIDs
            return null; // Return null to use generic message
        }
        
        // Remove any remaining entity class paths (e.g., eventplanner.security.auth.entity.UserAccount)
        message = message.replaceAll("\\[?[a-z]+(\\.[a-z]+)*\\.[A-Z][a-zA-Z]+#[a-f0-9-]+\\]?", "");
        
        // Remove UUIDs
        message = message.replaceAll("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", "");
        
        // Remove class paths
        message = message.replaceAll("[a-z]+(\\.[a-z]+)*\\.[A-Z][a-zA-Z]+", "");
        
        return message.trim().isEmpty() ? null : message.trim();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed")
                .path(request.getDescription(false).replace("uri=", ""))
                .validationErrors(errors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Invalid argument: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Invalid request parameters")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {

        log.warn("Malformed request payload: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Malformed request payload")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, WebRequest request) {

        log.warn("Missing required parameter: {}", ex.getParameterName());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message("Required parameter is missing")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message("The requested resource was not found")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {

        log.warn("Unauthorized access attempt: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Authentication required")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {

        log.warn("Access denied: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("Access denied")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            ForbiddenException ex, WebRequest request) {

        log.warn("Forbidden access attempt: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("Access denied")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException ex, WebRequest request) {

        log.warn("API exception: {}", ex.getMessage());

        // Use standard HTTP status text instead of custom error codes
        String errorText = getStandardHttpStatusText(ex.getStatus());
        
        // Sanitize message to prevent internal details exposure
        String sanitizedMessage = sanitizeMessage(ex.getMessage());
        if (sanitizedMessage == null) {
            sanitizedMessage = getGenericMessageForStatus(ex.getStatus());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus())
                .error(errorText)
                .message(sanitizedMessage)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(ex.getStatus()).body(errorResponse);
    }
    
    /**
     * Returns standard HTTP status text for the given status code.
     * Follows RFC 7231 and RFC 6585 standards.
     */
    private String getStandardHttpStatusText(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            default -> HttpStatus.valueOf(status).getReasonPhrase();
        };
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {

        log.warn("No handler found for: {}", ex.getRequestURL());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message("The requested resource was not found")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        
        log.warn("Method not supported: {} for {}", ex.getMethod(), request.getDescription(false));
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("Method Not Allowed")
                .message("The request method is not supported for this resource")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex, WebRequest request) {
        
        log.warn("Response status exception: {}", ex.getReason());
        
        // Sanitize the reason message
        String sanitizedMessage = sanitizeMessage(ex.getReason());
        if (sanitizedMessage == null) {
            sanitizedMessage = getGenericMessageForStatus(ex.getStatusCode().value());
        }
        
        String errorText = getStandardHttpStatusText(ex.getStatusCode().value());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatusCode().value())
                .error(errorText)
                .message(sanitizedMessage)
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex, WebRequest request) {
        
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message("The resource has been modified. Please refresh and try again.")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }


    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            org.springframework.dao.DataAccessException ex, WebRequest request) {
        
        log.error("Database access error while processing {}", request.getDescription(false), ex);
        
        // Check if it's a stale object/optimistic locking issue
        String exceptionMessage = ex.getMessage();
        if (exceptionMessage != null && (exceptionMessage.contains("Row was updated or deleted") || 
            exceptionMessage.contains("StaleObjectStateException") ||
            exceptionMessage.contains("optimistic lock") ||
            exceptionMessage.contains("unsaved-value mapping"))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.CONFLICT.value())
                    .error("Conflict")
                    .message("The resource has been modified. Please refresh and try again.")
                    .path(request.getDescription(false).replace("uri=", ""))
                    .build()
            );
        }
        
        // Never expose SQL details, entity names, IDs, or any internal details to clients
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("A database error occurred. Please try again.")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(java.sql.SQLException.class)
    public ResponseEntity<ErrorResponse> handleSQLException(
            java.sql.SQLException ex, WebRequest request) {
        
        log.error("SQL error while processing {}", request.getDescription(false), ex);
        
        // Never expose SQL details to clients, even in dev mode
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("A database error occurred. Please try again.")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Unhandled exception while processing {}", request.getDescription(false), ex);

        // NEVER expose internal details - always use generic message
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Returns a generic error message for a given HTTP status code.
     */
    private String getGenericMessageForStatus(int status) {
        return switch (status) {
            case 400 -> "Invalid request";
            case 401 -> "Authentication required";
            case 403 -> "Access denied";
            case 404 -> "The requested resource was not found";
            case 409 -> "A conflict occurred with the current state of the resource";
            case 429 -> "Too many requests. Please try again later";
            case 500 -> "An unexpected error occurred";
            default -> "An error occurred";
        };
    }
}
