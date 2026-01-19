package eventplanner.features.collaboration.dto.response;

import eventplanner.features.event.enums.EventUserType;
import eventplanner.features.collaboration.enums.EventPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for event collaborator information.
 */
@Schema(description = "Event collaborator response")
@Getter
@Setter
public class EventCollaboratorResponse {

    @Schema(description = "Collaborator ID")
    private UUID collaboratorId;

    @Schema(description = "Event ID")
    private UUID eventId;

    @Schema(description = "User ID")
    private UUID userId;

    @Schema(description = "User email")
    private String email;

    @Schema(description = "User name")
    private String userName;

    @Schema(description = "Collaborator role")
    private EventUserType role;

    @Schema(description = "Custom permissions")
    private List<EventPermission> permissions;

    @Schema(description = "Registration status")
    private String registrationStatus;

    @Schema(description = "Whether invitation was sent")
    private Boolean invitationSent;

    @Schema(description = "Invitation sent date")
    private LocalDateTime invitationSentAt;

    @Schema(description = "Collaborator added date")
    private LocalDateTime addedAt;

    @Schema(description = "Last updated date")
    private LocalDateTime updatedAt;
}




