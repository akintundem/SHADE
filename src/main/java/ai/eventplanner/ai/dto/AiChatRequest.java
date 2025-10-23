package ai.eventplanner.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequest {
    @NotBlank(message = "Message is required")
    private String message;
    
    private String userId = "anonymous";
    private String chatId = "default";
    private String eventId;
}
