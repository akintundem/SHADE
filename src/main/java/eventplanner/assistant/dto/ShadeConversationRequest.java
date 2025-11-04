package eventplanner.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for Shade AI assistant conversations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShadeConversationRequest {

    @NotBlank(message = "Message is required")
    private String message;

    private UUID sessionId;

    private Map<String, Object> context;

    private String intent;

    private Map<String, Object> collectedData;
}
