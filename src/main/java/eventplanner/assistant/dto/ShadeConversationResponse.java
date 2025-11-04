package eventplanner.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for Shade AI assistant conversations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShadeConversationResponse {

    private UUID sessionId;
    
    private String message;
    
    private String intent;
    
    private Map<String, Object> collectedData;
    
    private List<String> missingFields;
    
    private List<String> followUpQuestions;
    
    private Map<String, List<String>> suggestions;
    
    private ShadeAction action;
    
    private OffsetDateTime timestamp;
    
    private boolean requiresConfirmation;
    
    private String confirmationMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShadeAction {
        private String type; // "createEvent", "updateEvent", "listEvents", "deleteEvent", "none"
        private Map<String, Object> arguments;
        private boolean ready;
    }
}
