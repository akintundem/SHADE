package ai.eventplanner.event.dto.response;

import ai.eventplanner.common.domain.enums.EventStatus;
import ai.eventplanner.common.domain.enums.EventType;
import ai.eventplanner.common.domain.enums.EventUserType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user-event relationship information
 */
@Schema(description = "User event relationship response")
@Getter
@Setter
public class UserEventRelationshipResponse {

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "Event name")
    private String eventName;

    @Schema(description = "Event description")
    private String eventDescription;

    @Schema(description = "Event type")
    private EventType eventType;

    @Schema(description = "Event status")
    private EventStatus eventStatus;

    @Schema(description = "Start date and time")
    private LocalDateTime startDateTime;

    @Schema(description = "End date and time")
    private LocalDateTime endDateTime;

    @Schema(description = "User's role in the event")
    private EventUserType userRole;

    @Schema(description = "Registration status")
    private String registrationStatus;

    @Schema(description = "Registration date")
    private LocalDateTime registrationDate;

    @Schema(description = "Whether user is the owner")
    private Boolean isOwner;

    @Schema(description = "Event capacity")
    private Integer capacity;

    @Schema(description = "Current attendee count")
    private Integer currentAttendeeCount;

    @Schema(description = "Whether event is public")
    private Boolean isPublic;

    @Schema(description = "Cover image URL")
    private String coverImageUrl;

    @Schema(description = "Event website URL")
    private String eventWebsiteUrl;

    @Schema(description = "Event hashtag")
    private String hashtag;
}
