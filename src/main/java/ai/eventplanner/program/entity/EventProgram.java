package ai.eventplanner.program.entity;

import ai.eventplanner.event.entity.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event program and content management.
 */
@Entity
@Table(name = "event_programs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "program_type")
    private ProgramType programType;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "location")
    private String location;

    @Column(name = "speaker_name")
    private String speakerName;

    @Column(name = "speaker_title")
    private String speakerTitle;

    @Column(name = "speaker_bio", columnDefinition = "TEXT")
    private String speakerBio;

    @Column(name = "speaker_email")
    private String speakerEmail;

    @Column(name = "speaker_phone")
    private String speakerPhone;

    @Column(name = "presentation_title")
    private String presentationTitle;

    @Column(name = "presentation_description", columnDefinition = "TEXT")
    private String presentationDescription;

    @Column(name = "presentation_url")
    private String presentationUrl;

    @Column(name = "handouts_url")
    private String handoutsUrl;

    @Column(name = "equipment_required", columnDefinition = "TEXT")
    private String equipmentRequired;

    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_keynote")
    @Builder.Default
    private Boolean isKeynote = false;

    @Column(name = "is_breakout")
    @Builder.Default
    private Boolean isBreakout = false;

    @Column(name = "max_attendees")
    private Integer maxAttendees;

    @Column(name = "current_attendees")
    @Builder.Default
    private Integer currentAttendees = 0;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ProgramType {
        KEYNOTE,
        PANEL,
        WORKSHOP,
        BREAKOUT,
        PERFORMANCE,
        MEAL,
        NETWORKING,
        ENTERTAINMENT,
        OTHER
    }
}
