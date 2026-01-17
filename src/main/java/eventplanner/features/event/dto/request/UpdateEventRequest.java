package eventplanner.features.event.dto.request;

import eventplanner.features.event.enums.EventAccessType;
import eventplanner.features.event.enums.EventStatus;
import eventplanner.features.event.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class UpdateEventRequest {

    @Size(max = 255, message = "Event name must not exceed 255 characters")
    @Schema(description = "Updated event name")
    private String name;

    @Size(max = 10000, message = "Description is too long")
    @Schema(description = "Updated event description")
    private String description;

    @Schema(description = "Updated event type")
    private EventType eventType;

    @Schema(description = "Updated event status")
    private EventStatus eventStatus;

    @Schema(description = "Updated start date and time")
    private LocalDateTime startDateTime;

    @Schema(description = "Updated end date and time")
    private LocalDateTime endDateTime;

    @Schema(description = "Updated registration deadline")
    private LocalDateTime registrationDeadline;

    @Schema(description = "Updated capacity")
    private Integer capacity;

    @Schema(description = "Updated attendee count")
    private Integer currentAttendeeCount;

    @Schema(description = "Updated visibility")
    private Boolean isPublic;

    @Schema(description = "Updated approval requirement")
    private Boolean requiresApproval;

    @Schema(description = "Updated cover image URL")
    private String coverImageUrl;

    @Schema(description = "Updated website URL")
    private String eventWebsiteUrl;

    @Schema(description = "Updated hashtag")
    private String hashtag;

    @Schema(description = "Updated theme")
    private String theme;

    @Schema(description = "Updated objectives")
    private String objectives;

    @Schema(description = "Updated target audience")
    private String targetAudience;

    @Schema(description = "Updated success metrics")
    private String successMetrics;

    @Schema(description = "Updated branding guidelines")
    private String brandingGuidelines;

    @Schema(description = "Updated venue requirements")
    private String venueRequirements;

    @Schema(description = "Updated technical requirements")
    private String technicalRequirements;

    @Schema(description = "Updated accessibility features")
    private String accessibilityFeatures;

    @Schema(description = "Updated emergency plan")
    private String emergencyPlan;

    @Schema(description = "Updated backup plan")
    private String backupPlan;

    @Schema(description = "Updated post-event tasks")
    private String postEventTasks;

    @Schema(description = "Updated metadata")
    private String metadata;

    @Schema(description = "Owner identifier override")
    private UUID ownerId;

    @Schema(description = "Venue identifier override")
    private UUID venueId;

    @Schema(description = "Whether to remove the venue association")
    private Boolean venueCleared;
    
    @Schema(description = "Venue information with location details")
    private eventplanner.features.event.dto.VenueDTO venue;

    // ============ ACCESS CONTROL SETTINGS ============

    @Schema(description = "How users can access this event's content. " +
            "OPEN: Anyone can view and RSVP. " +
            "RSVP_REQUIRED: Users must RSVP to access content. " +
            "INVITE_ONLY: Only invited users can see/access the event. " +
            "TICKETED: Users must purchase a ticket to access content.")
    private EventAccessType accessType;

    @Schema(description = "Whether feeds should be made public after the event ends. " +
            "Applicable for RSVP_REQUIRED, INVITE_ONLY, and TICKETED events.")
    private Boolean feedsPublicAfterEvent;

    public boolean isVenueCleared() {
        return Boolean.TRUE.equals(venueCleared);
    }
}
