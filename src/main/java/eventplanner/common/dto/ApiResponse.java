package eventplanner.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standard API response wrapper for all successful endpoints.
 * Provides consistent structure with metadata across the entire API.
 *
 * All successful responses return this format:
 * {
 *   "success": true,
 *   "data": {...},
 *   "pagination": {...},  // Optional, for paginated responses
 *   "timestamp": "2026-01-20T10:30:00Z"
 * }
 *
 * Error responses use Zalando Problem (RFC 7807) format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * Indicates success (always true for successful responses)
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Response data (business payload)
     */
    private T data;

    /**
     * Pagination metadata (only for paginated list responses)
     */
    private PaginationMetadata pagination;

    /**
     * Response timestamp (ISO-8601 format)
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Request ID for tracing (optional)
     */
    private String requestId;

    /**
     * Additional metadata (optional)
     */
    private Map<String, Object> metadata;

    /**
     * Create successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create successful response with data and pagination
     */
    public static <T> ApiResponse<T> success(T data, PaginationMetadata pagination) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .pagination(pagination)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create successful response with data and request ID
     */
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .requestId(requestId)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Create empty successful response (for 204 No Content, etc.)
     */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
            .success(true)
            .timestamp(Instant.now())
            .build();
    }
}
