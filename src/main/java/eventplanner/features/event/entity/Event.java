package eventplanner.features.event.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.event.enums.EventAccessType;
import eventplanner.features.event.enums.EventStatus;
import eventplanner.features.event.enums.EventType;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core event entity. Relationships to other bounded contexts are represented
 * via identifier fields to keep the module decoupled.
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

    /**
     * Many-to-one relationship with the user who owns this event.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

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

    @Column(name = "venue_id")
    private UUID venueId;
    
    @Embedded
    private Venue venue;

    /**
     * Sync embedded venue's PostGIS Point before persist/update.
     */
    @PrePersist
    @PreUpdate
    private void syncVenueLocation() {
        if (venue != null) {
            venue.syncLocation();
        }
    }

    // Platform payment tracking
    @Column(name = "platform_payment_id")
    private UUID platformPaymentId;
    
    @Column(name = "creation_fee_paid")
    private Boolean creationFeePaid = false;
    
    @Column(name = "creation_fee_amount", precision = 10, scale = 2)
    private java.math.BigDecimal creationFeeAmount;
    
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    // Event Access Control Settings
    /**
     * Defines how users can access this event's content and participate.
     * - OPEN: Anyone can view and RSVP (default for public events)
     * - RSVP_REQUIRED: Users must RSVP to access content
     * - INVITE_ONLY: Only invited users can see/access the event
     * - TICKETED: Users must purchase a ticket to access content
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "access_type", nullable = false)
    private EventAccessType accessType = EventAccessType.OPEN;

    /**
     * Whether feeds should be made public after the event ends (status = COMPLETED).
     * Applicable for RSVP_REQUIRED, INVITE_ONLY, and TICKETED events.
     * If true, feeds become publicly viewable after the event completes.
     * If false, feeds remain restricted to authorized users even after the event.
     */
    @Column(name = "feeds_public_after_event", nullable = false)
    private Boolean feedsPublicAfterEvent = false;

    // Timeline publication state
    @Column(name = "timeline_published", nullable = false)
    private Boolean timelinePublished = false;

    @Column(name = "timeline_published_at")
    private LocalDateTime timelinePublishedAt;

    /**
     * Many-to-one relationship with the user who published the timeline.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timeline_published_by")
    private UserAccount timelinePublishedBy;

    @Column(name = "timeline_publish_message", columnDefinition = "TEXT")
    private String timelinePublishMessage;

    // Archive/restore fields
    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    /**
     * Many-to-one relationship with the user who archived this event.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archived_by")
    private UserAccount archivedBy;

    @Column(name = "archive_reason", columnDefinition = "TEXT")
    private String archiveReason;

    @Column(name = "restored_at")
    private LocalDateTime restoredAt;

    /**
     * Many-to-one relationship with the user who restored this event.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restored_by")
    private UserAccount restoredBy;

    // ==================== RELATIONSHIPS ====================

    /**
     * One-to-many relationship with stored objects (media, assets, etc.).
     * Lazy loaded to avoid N+1 queries.
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EventStoredObject> storedObjects = new ArrayList<>();

    /**
     * One-to-many relationship with event reminders.
     * Lazy loaded to avoid N+1 queries.
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EventReminder> reminders = new ArrayList<>();

    /**
     * One-to-one relationship with notification settings.
     */
    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private EventNotificationSettings notificationSettings;

    /**
     * One-to-many relationship with feed posts.
     * Lazy loaded to avoid N+1 queries.
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<eventplanner.features.feeds.entity.EventFeedPost> feedPosts = new ArrayList<>();

    /**
     * One-to-many relationship with tickets.
     * Note: orphanRemoval = false to support soft-delete scenarios.
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<eventplanner.features.ticket.entity.Ticket> tickets = new ArrayList<>();

    /**
     * One-to-many relationship with attendees.
     * Note: orphanRemoval = false to preserve attendee history.
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<eventplanner.features.attendee.entity.Attendee> attendees = new ArrayList<>();

    /**
     * One-to-many relationship with ticket types.
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<eventplanner.features.ticket.entity.TicketType> ticketTypes = new ArrayList<>();

    /**
     * One-to-many relationship with waitlist entries.
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<eventplanner.features.event.entity.EventWaitlistEntry> waitlistEntries = new ArrayList<>();

    public Event(String name, EventType eventType, UserAccount owner) {
        this.name = name;
        this.eventType = eventType;
        this.owner = owner;
    }

}
