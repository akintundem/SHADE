package eventplanner.features.event.dto.response;

import eventplanner.features.event.enums.EventUserType;
import eventplanner.features.collaboration.entity.EventCollaboratorInvite;
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

    /**
     * Create a CollaboratorInviteResponse from an EventCollaboratorInvite entity.
     */
    public static CollaboratorInviteResponse from(EventCollaboratorInvite invite) {
        CollaboratorInviteResponse res = new CollaboratorInviteResponse();
        res.setInviteId(invite.getId());
        res.setEventId(invite.getEvent() != null ? invite.getEvent().getId() : null);
        res.setInviterUserId(invite.getInviter() != null ? invite.getInviter().getId() : null);
        res.setInviteeUserId(invite.getInvitee() != null ? invite.getInvitee().getId() : null);
        res.setInviteeEmail(invite.getInviteeEmail());
        res.setRole(invite.getRole());
        res.setStatus(invite.getStatus());
        res.setExpiresAt(invite.getExpiresAt());
        res.setRespondedAt(invite.getRespondedAt());
        res.setMessage(invite.getMessage());
        res.setCreatedAt(invite.getCreatedAt());
        res.setUpdatedAt(invite.getUpdatedAt());
        return res;
    }
}

