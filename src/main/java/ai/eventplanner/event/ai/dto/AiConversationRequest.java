package ai.eventplanner.event.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiConversationRequest {

    @NotBlank(message = "Message is required")
    private String message;
}
