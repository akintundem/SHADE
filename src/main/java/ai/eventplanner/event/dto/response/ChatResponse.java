package ai.eventplanner.event.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for AI chat interactions
 */
@Schema(description = "AI chat response")
public class ChatResponse {

    @Schema(description = "AI response message", example = "I'll help you plan a professional corporate conference for 200 attendees with a $50,000 budget.")
    private String message;

    @Schema(description = "Timestamp of the response", example = "2024-01-15T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Unique identifier for this chat interaction", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID interactionId;

    @Schema(description = "Detected event type from the conversation", example = "CONFERENCE")
    private String detectedEventType;

    @Schema(description = "Confidence score for event type detection", example = "0.95")
    private Double confidenceScore;

    @Schema(description = "Suggested next steps or actions")
    private List<String> suggestedActions;

    @Schema(description = "Additional context or recommendations")
    private String context;

    @Schema(description = "Whether the AI needs more information", example = "false")
    private Boolean needsMoreInfo;

    @Schema(description = "Follow-up questions from the AI")
    private List<String> followUpQuestions;

    // Backward compatibility methods
    public String getReply() {
        return message;
    }

    public void setReply(String reply) {
        this.message = reply;
    }

    // Constructors
    public ChatResponse() {}

    public ChatResponse(String message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public ChatResponse(String message, LocalDateTime timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public UUID getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(UUID interactionId) {
        this.interactionId = interactionId;
    }

    public String getDetectedEventType() {
        return detectedEventType;
    }

    public void setDetectedEventType(String detectedEventType) {
        this.detectedEventType = detectedEventType;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public List<String> getSuggestedActions() {
        return suggestedActions;
    }

    public void setSuggestedActions(List<String> suggestedActions) {
        this.suggestedActions = suggestedActions;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Boolean getNeedsMoreInfo() {
        return needsMoreInfo;
    }

    public void setNeedsMoreInfo(Boolean needsMoreInfo) {
        this.needsMoreInfo = needsMoreInfo;
    }

    public List<String> getFollowUpQuestions() {
        return followUpQuestions;
    }

    public void setFollowUpQuestions(List<String> followUpQuestions) {
        this.followUpQuestions = followUpQuestions;
    }
}