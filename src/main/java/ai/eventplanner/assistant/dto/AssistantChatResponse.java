package ai.eventplanner.assistant.dto;

import lombok.Data;
import java.util.Map;

/**
 * Unified DTO for responses coming from the Python assistant, structured for mobile routing.
 */
@Data
public class AssistantChatResponse {
    private String reply;
    private String toolUsed;
    private Map<String, Object> data;
    private boolean showChips;
    private String chatId;
    private String userId;
    private String eventId;
    private Map<String, Object> ui;
    private StructuredResponseDTO structuredResponse;
    private String uitype; // "chat", "venue_cards", "email_template", "chips", "mixed"
    private boolean success = true;
    private String error;
}


