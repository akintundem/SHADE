package eventplanner.features.attendee.dto.response;

import eventplanner.features.attendee.entity.AttendeeInvite;
import eventplanner.features.attendee.enums.AttendeeInviteStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AttendeeInviteResponse {
    private UUID inviteId;
    private UUID eventId;
    private UUID inviterUserId;
    private UUID inviteeUserId;
    private String inviteeEmail;
    private AttendeeInviteStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime respondedAt;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AttendeeInviteResponse from(AttendeeInvite invite) {
        AttendeeInviteResponse res = new AttendeeInviteResponse();
        res.setInviteId(invite.getId());
        res.setEventId(invite.getEvent() != null ? invite.getEvent().getId() : null);
        res.setInviterUserId(invite.getInviter() != null ? invite.getInviter().getId() : null);
        res.setInviteeUserId(invite.getInvitee() != null ? invite.getInvitee().getId() : null);
        res.setInviteeEmail(invite.getInviteeEmail());
        res.setStatus(invite.getStatus());
        res.setExpiresAt(invite.getExpiresAt());
        res.setRespondedAt(invite.getRespondedAt());
        res.setMessage(invite.getMessage());
        res.setCreatedAt(invite.getCreatedAt());
        res.setUpdatedAt(invite.getUpdatedAt());
        return res;
    }
}
