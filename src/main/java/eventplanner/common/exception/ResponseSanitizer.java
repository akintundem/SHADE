package eventplanner.common.exception;

import java.util.regex.Pattern;

/**
 * Utility class to detect and sanitize sensitive information in API responses.
 * STRICT SECURITY POLICY: Never expose internal implementation details to clients.
 */
public final class ResponseSanitizer {

    private ResponseSanitizer() {}
    
    private static final Pattern PACKAGE_PATH_PATTERN = Pattern.compile(
            "[a-z]+\\.[a-z]+\\.[a-z]+\\.[A-Z][a-zA-Z]+");
    
    private static final Pattern ENTITY_ID_PATTERN = Pattern.compile(
            "\\[?[a-z]+(\\.[a-z]+)*\\.[A-Z][a-zA-Z]+#[a-f0-9-]+\\]?");

    // Keywords that indicate sensitive internal details
    private static final String[] SENSITIVE_KEYWORDS = {
        // Database/ORM
        "hibernate", "jpa", "jdbc", "sql", "persistence", "entity", "repository",
        "transaction", "rollback", "commit", "deadlock", "constraint violation",
        "foreign key", "primary key", "duplicate key", "unique constraint",
        
        // Hibernate specific errors
        "row was updated or deleted", "staleobjectstate", "stalestate", 
        "optimistic lock", "unsaved-value", "mapping was incorrect",
        "lazy initialization", "no session", "detached entity",
        "transient instance", "persistent instance",
        
        // Spring internals
        "springframework", "spring.", "bean", "autowired", "injection",
        "applicationcontext", "dispatcher", "servlet",
        
        // Java internals  
        "java.lang.", "java.util.", "java.io.", "java.sql.",
        "stacktrace", "stack trace", "caused by:", "at ",
        "nullpointerexception", "classcastexception", "illegalstateexception",
        "nosuchelementexception", "indexoutofboundsexception",
        
        // Security sensitive
        "password", "secret", "token", "credential", "apikey", "api_key",
        "private key", "encryption", "decrypt",
        
        // Infrastructure
        "connection refused", "connection reset", "timeout", "socket",
        "redis", "postgres", "mysql", "mongodb", "elasticsearch",
        "aws", "s3", "cognito", "lambda",
        
        // Code structure hints
        "controller", "service", "mapper", "dto", "impl",
        "eventplanner.", "com.amazonaws."
    };

    /**
     * Check if a string contains sensitive internal details that should not be exposed.
     */
    public static boolean containsSensitiveInfo(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();

        // Check for sensitive keywords
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }

        // Check for entity ID pattern (e.g., UserAccount#uuid)
        if (text.contains("#") && ENTITY_ID_PATTERN.matcher(text).find()) {
            return true;
        }

        // Check for package paths (e.g., eventplanner.security.auth.entity.UserAccount)
        if (PACKAGE_PATH_PATTERN.matcher(text).find()) {
            return true;
        }

        return false;
    }

    /**
     * Check if an error message specifically looks like a leaked exception.
     */
    public static boolean isLeakedExceptionMessage(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();

        // Common exception message patterns
        return lowerMessage.contains("exception") ||
               lowerMessage.contains("error:") ||
               lowerMessage.contains("failed to") ||
               lowerMessage.contains("unable to") ||
               lowerMessage.contains("cannot ") ||
               lowerMessage.contains("could not") ||
               lowerMessage.contains("null pointer") ||
               lowerMessage.contains("class cast") ||
               lowerMessage.contains("no such") ||
               lowerMessage.contains("not found:") ||
               (message.contains(".") && message.contains("(") && message.contains(")")) || // Stack trace line
               containsSensitiveInfo(message);
    }

    /**
     * Returns a safe, generic error message for the given HTTP status.
     */
    public static String getGenericMessage(int status) {
        return switch (status) {
            case 400 -> "Invalid request";
            case 401 -> "Authentication required";
            case 403 -> "Access denied";
            case 404 -> "The requested resource was not found";
            case 405 -> "The request method is not supported";
            case 409 -> "The resource has been modified. Please refresh and try again.";
            case 429 -> "Too many requests. Please try again later.";
            case 500, 502, 503, 504 -> "An unexpected error occurred";
            default -> "An error occurred";
        };
    }

    /**
     * Sanitize a message - returns generic message if sensitive info detected.
     */
    public static String sanitize(String message, int status) {
        if (message == null || containsSensitiveInfo(message) || isLeakedExceptionMessage(message)) {
            return getGenericMessage(status);
        }
        return message;
    }
}
