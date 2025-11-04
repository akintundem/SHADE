package eventplanner.features.event.dto.response;

import eventplanner.common.domain.enums.EventStatus;
import eventplanner.common.domain.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for event information aligned with the Event entity
 */
@Schema(description = "Event information response")
@Getter
@Setter
public class EventResponse {

    @Schema(description = "Unique identifier of the event")
    private UUID id;

    @Schema(description = "Name of the event")
    private String name;

    @Schema(description = "Detailed description of the event")
    private String description;

    @Schema(description = "Type of the event")
    private EventType eventType;

    @Schema(description = "Current status of the event")
    private EventStatus eventStatus;

    @Schema(description = "Start date and time of the event")
    private LocalDateTime startDateTime;

    @Schema(description = "End date and time of the event")
    private LocalDateTime endDateTime;

    @Schema(description = "Registration deadline for the event")
    private LocalDateTime registrationDeadline;

    @Schema(description = "Total capacity of the event")
    private Integer capacity;

    @Schema(description = "Current attendee count")
    private Integer currentAttendeeCount;

    @Schema(description = "Whether the event is public")
    private Boolean isPublic;

    @Schema(description = "Whether the event requires approval")
    private Boolean requiresApproval;

    @Schema(description = "Whether QR codes are enabled")
    private Boolean qrCodeEnabled;

    @Schema(description = "QR code value")
    private String qrCode;

    @Schema(description = "Cover image URL for the event")
    private String coverImageUrl;

    @Schema(description = "External website URL for the event")
    private String eventWebsiteUrl;

    @Schema(description = "Event hashtag")
    private String hashtag;

    @Schema(description = "Theme for the event")
    private String theme;

    @Schema(description = "Objectives of the event")
    private String objectives;

    @Schema(description = "Target audience description")
    private String targetAudience;

    @Schema(description = "Success metrics for the event")
    private String successMetrics;

    @Schema(description = "Branding guidelines to follow")
    private String brandingGuidelines;

    @Schema(description = "Venue requirements")
    private String venueRequirements;

    @Schema(description = "Technical requirements")
    private String technicalRequirements;

    @Schema(description = "Accessibility features")
    private String accessibilityFeatures;

    @Schema(description = "Emergency plan")
    private String emergencyPlan;

    @Schema(description = "Backup plan")
    private String backupPlan;

    @Schema(description = "Post-event tasks")
    private String postEventTasks;

    @Schema(description = "Serialized metadata")
    private String metadata;

    @Schema(description = "Owner identifier")
    private UUID ownerId;

    @Schema(description = "Venue identifier")
    private UUID venueId;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last updated timestamp")
    private LocalDateTime updatedAt;
}
