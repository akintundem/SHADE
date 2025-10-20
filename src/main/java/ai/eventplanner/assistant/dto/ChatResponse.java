package ai.eventplanner.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for chat messages from the AI assistant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String reply;

    private String toolUsed;

    private Map<String, Object> data;

    private String conversationState;

    private boolean showChips;

    private List<String> chips;

    private String eventId;

    private String chatId;

    private String userId;

    private boolean toolCallRequired;

    private String toolName;

    private List<Map<String, Object>> venues;

    private String error;

    private boolean success;
}
