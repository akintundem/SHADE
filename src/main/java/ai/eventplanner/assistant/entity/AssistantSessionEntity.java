package ai.eventplanner.assistant.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "assistant_sessions")
public class AssistantSessionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "domain", nullable = false, length = 64)
    private String domain;

    @Column(name = "organizer_id")
    private UUID organizerId;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "session_type", length = 100)
    private String type;

    @Column(name = "session_date")
    private OffsetDateTime date;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "guest_count")
    private Integer guestCount;

    @Column(name = "budget", precision = 19, scale = 2)
    private BigDecimal budget;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "assistant_session_preferences", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "preference", length = 255)
    private List<String> preferences = new ArrayList<>();

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated;

    @Lob
    @Column(name = "context_payload")
    private String contextPayload;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
