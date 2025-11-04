package eventplanner.features.event.dto.request;

import eventplanner.common.domain.enums.EventStatus;
import eventplanner.common.domain.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request DTO for creating a new event aligned with the Event entity
 */
@Schema(description = "Request to create a new event")
@Getter
@Setter
public class CreateEventRequest {

    @NotBlank(message = "Event name is required")
    @Size(max = 255, message = "Event name must not exceed 255 characters")
    @Schema(description = "Name of the event", example = "Annual Company Conference", required = true)
    private String name;

    @Size(max = 10000, message = "Description is too long")
    @Schema(description = "Detailed description of the event")
    private String description;

    @NotNull(message = "Event type is required")
    @Schema(description = "Type of the event", example = "CONFERENCE", required = true)
    private EventType eventType;

    @Schema(description = "Status of the event", example = "PLANNING")
    private EventStatus eventStatus;

    @FutureOrPresent(message = "Start date must be in the present or future")
    @Schema(description = "Start date and time of the event", example = "2024-06-15T09:00:00")
    private LocalDateTime startDateTime;

    @Schema(description = "End date and time of the event", example = "2024-06-15T17:00:00")
    private LocalDateTime endDateTime;

    @Schema(description = "Registration deadline for the event", example = "2024-06-10T23:59:59")
    private LocalDateTime registrationDeadline;

    @Schema(description = "Total capacity of the event", example = "200")
    private Integer capacity;

    @Schema(description = "Current attendee count", example = "25")
    private Integer currentAttendeeCount;

    @Schema(description = "Whether the event is public", example = "true")
    private Boolean isPublic;

    @Schema(description = "Whether the event requires approval", example = "false")
    private Boolean requiresApproval;

    @Schema(description = "Whether QR codes are enabled", example = "true")
    private Boolean qrCodeEnabled;

    @Schema(description = "QR code content or reference")
    private String qrCode;

    @Schema(description = "Cover image URL for the event")
    private String coverImageUrl;

    @Schema(description = "External website URL for the event")
    private String eventWebsiteUrl;

    @Schema(description = "Event hashtag", example = "#Inspire2024")
    private String hashtag;

    @Schema(description = "Event theme details")
    private String theme;

    @Schema(description = "Event objectives")
    private String objectives;

    @Schema(description = "Target audience description")
    private String targetAudience;

    @Schema(description = "Success metrics for the event")
    private String successMetrics;

    @Schema(description = "Brand guidelines to follow")
    private String brandingGuidelines;

    @Schema(description = "Venue requirements")
    private String venueRequirements;

    @Schema(description = "Technical requirements")
    private String technicalRequirements;

    @Schema(description = "Accessibility features")
    private String accessibilityFeatures;

    @Schema(description = "Emergency plan details")
    private String emergencyPlan;

    @Schema(description = "Backup plan details")
    private String backupPlan;

    @Schema(description = "Post-event tasks")
    private String postEventTasks;

    @Schema(description = "Serialized metadata for the event")
    private String metadata;
}
