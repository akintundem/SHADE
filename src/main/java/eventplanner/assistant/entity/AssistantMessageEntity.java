package eventplanner.assistant.entity;

import eventplanner.common.domain.enums.MessageRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "assistant_messages")
public class AssistantMessageEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ElementCollection
    @CollectionTable(name = "assistant_message_suggestions", joinColumns = @JoinColumn(name = "message_id"))
    private List<AssistantMessageSuggestion> suggestions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "assistant_message_followups", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "follow_up", length = 255)
    private List<String> followUpQuestions = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public static AssistantMessageEntity userMessage(UUID sessionId, String content) {
        AssistantMessageEntity message = new AssistantMessageEntity();
        message.id = UUID.randomUUID();
        message.sessionId = sessionId;
        message.role = MessageRole.USER;
        message.content = content;
        message.createdAt = OffsetDateTime.now();
        return message;
    }

    public static AssistantMessageEntity assistantMessage(UUID sessionId,
                                                          String content,
                                                          List<String> followUps,
                                                          Map<String, List<String>> suggestions) {
        AssistantMessageEntity message = new AssistantMessageEntity();
        message.id = UUID.randomUUID();
        message.sessionId = sessionId;
        message.role = MessageRole.ASSISTANT;
        message.content = content;
        if (followUps != null) {
            message.followUpQuestions = new ArrayList<>(followUps);
        }
        if (suggestions != null && !suggestions.isEmpty()) {
            suggestions.forEach((category, values) -> {
                if (values != null) {
                    values.forEach(value -> message.suggestions.add(new AssistantMessageSuggestion(category, value)));
                }
            });
        }
        message.createdAt = OffsetDateTime.now();
        return message;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (followUpQuestions == null) {
            followUpQuestions = new ArrayList<>();
        }
        if (suggestions == null) {
            suggestions = new ArrayList<>();
        }
    }
}
