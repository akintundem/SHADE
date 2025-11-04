package eventplanner.common.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Minimal success/message response payload for simple operations.
 */
@Value
@Builder
public class ApiMessageResponse {
    boolean success;
    String message;

    public static ApiMessageResponse success(String message) {
        return ApiMessageResponse.builder()
            .success(true)
            .message(message)
            .build();
    }

    public static ApiMessageResponse failure(String message) {
        return ApiMessageResponse.builder()
            .success(false)
            .message(message)
            .build();
    }
}
