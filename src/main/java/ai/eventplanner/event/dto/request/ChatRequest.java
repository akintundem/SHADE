package ai.eventplanner.event.dto.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for AI chat interaction
 */
@Schema(description = "Request for AI chat interaction")
public class ChatRequest {

    @NotBlank(message = "Message is required")
    @Size(min = 1, max = 2000, message = "Message must be between 1 and 2000 characters")
    @Schema(description = "User message to the AI", example = "I need help planning a corporate conference for 200 people", required = true)
    private String message;

    @Size(max = 100, message = "Context cannot exceed 100 characters")
    @Schema(description = "Additional context for the conversation", example = "budget planning")
    private String context;

    @Schema(description = "Whether to include conversation history", example = "true")
    private Boolean includeHistory = true;

    @Min(value = 1, message = "Max history must be at least 1")
    @Max(value = 50, message = "Max history cannot exceed 50")
    @Schema(description = "Maximum number of previous messages to include", example = "10", minimum = "1", maximum = "50")
    private Integer maxHistory = 10;

    // Constructors
    public ChatRequest() {}

    public ChatRequest(String message) {
        this.message = message;
    }

    public ChatRequest(String message, String context) {
        this.message = message;
        this.context = context;
    }

    // Getters and setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Boolean getIncludeHistory() {
        return includeHistory;
    }

    public void setIncludeHistory(Boolean includeHistory) {
        this.includeHistory = includeHistory;
    }

    public Integer getMaxHistory() {
        return maxHistory;
    }

    public void setMaxHistory(Integer maxHistory) {
        this.maxHistory = maxHistory;
    }
}