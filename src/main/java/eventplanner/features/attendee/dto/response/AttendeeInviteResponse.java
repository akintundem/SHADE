package eventplanner.features.attendee.dto.response;

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
}
