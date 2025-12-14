package eventplanner.features.event.dto.response;

import eventplanner.common.domain.enums.EventUserType;
import eventplanner.features.collaboration.enums.CollaboratorInviteStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CollaboratorInviteResponse {
    private UUID inviteId;
    private UUID eventId;
    private UUID inviterUserId;
    private UUID inviteeUserId;
    private String inviteeEmail;
    private EventUserType role;
    private CollaboratorInviteStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime respondedAt;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

