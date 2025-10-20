package ai.eventplanner.event.dto.request;

import ai.eventplanner.common.domain.enums.EventStatus;
import ai.eventplanner.common.domain.enums.EventType;
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

    @Schema(description = "Updated QR code flag")
    private Boolean qrCodeEnabled;

    @Schema(description = "Updated QR code")
    private String qrCode;

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

    public boolean isVenueCleared() {
        return Boolean.TRUE.equals(venueCleared);
    }
}
