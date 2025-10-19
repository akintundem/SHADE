package ai.eventplanner.event.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import ai.eventplanner.common.domain.enums.EventStatus;
import ai.eventplanner.common.domain.enums.EventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core event entity covering all aspects of event planning
 */
@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Event extends BaseEntity {
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType eventType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_status")
    private EventStatus eventStatus = EventStatus.PLANNING;
    
    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;
    
    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;
    
    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;
    
    @Column(name = "capacity")
    private Integer capacity;
    
    @Column(name = "current_attendee_count")
    private Integer currentAttendeeCount = 0;
    
    @Column(name = "is_public")
    private Boolean isPublic = true;
    
    @Column(name = "requires_approval")
    private Boolean requiresApproval = false;
    
    @Column(name = "qr_code_enabled")
    private Boolean qrCodeEnabled = false;
    
    @Column(name = "qr_code")
    private String qrCode;
    
    @Column(name = "cover_image_url")
    private String coverImageUrl;
    
    @Column(name = "event_website_url")
    private String eventWebsiteUrl;
    
    @Column(name = "hashtag")
    private String hashtag;
    
    @Column(name = "theme", columnDefinition = "TEXT")
    private String theme;
    
    @Column(name = "objectives", columnDefinition = "TEXT")
    private String objectives;
    
    @Column(name = "target_audience", columnDefinition = "TEXT")
    private String targetAudience;
    
    @Column(name = "success_metrics", columnDefinition = "TEXT")
    private String successMetrics;
    
    @Column(name = "branding_guidelines", columnDefinition = "TEXT")
    private String brandingGuidelines;
    
    @Column(name = "venue_requirements", columnDefinition = "TEXT")
    private String venueRequirements;
    
    @Column(name = "technical_requirements", columnDefinition = "TEXT")
    private String technicalRequirements;
    
    @Column(name = "accessibility_features", columnDefinition = "TEXT")
    private String accessibilityFeatures;
    
    @Column(name = "emergency_plan", columnDefinition = "TEXT")
    private String emergencyPlan;
    
    @Column(name = "backup_plan", columnDefinition = "TEXT")
    private String backupPlan;
    
    @Column(name = "post_event_tasks", columnDefinition = "TEXT")
    private String postEventTasks;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private EventUser organizer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private ServiceProvider venue;
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventAttendance> attendances;
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventRole> roles;
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventVendor> vendors;
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Budget> budgets;
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TimelineItem> timelineItems;
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventRisk> risks;
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Communication> communications;
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventProgram> programs;
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventChecklist> checklists;
    
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConversationHistory> conversations;
    
    public Event(String name, EventType eventType, EventUser organizer) {
        this.name = name;
        this.eventType = eventType;
        this.organizer = organizer;
    }
}
