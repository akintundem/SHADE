package ai.eventplanner.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    private String reply;
    private String toolUsed;
    private Object data;
    private boolean showChips;
    private String chatId;
    private String userId;
    private String eventId;
    private Object ui;
    private Object structuredResponse;
}
