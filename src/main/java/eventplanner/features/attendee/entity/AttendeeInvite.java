package eventplanner.features.attendee.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.attendee.enums.AttendeeInviteStatus;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "attendee_invites",
        indexes = {
                @Index(name = "idx_attendee_inv_event_id", columnList = "event_id"),
                @Index(name = "idx_attendee_inv_invitee_user_id", columnList = "invitee_user_id"),
                @Index(name = "idx_attendee_inv_invitee_email", columnList = "invitee_email"),
                @Index(name = "idx_attendee_inv_status", columnList = "status"),
                @Index(name = "idx_attendee_inv_token_hash", columnList = "token_hash")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AttendeeInvite extends BaseEntity {

    /**
     * Many-to-one relationship with the event this invite is for.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * Many-to-one relationship with the user who sent the invite.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_user_id", nullable = false)
    private UserAccount inviter;

    /**
     * Many-to-one relationship with the user who was invited (optional, may be null if invite is by email).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_user_id")
    private UserAccount invitee;

    @Column(name = "invitee_email", length = 180)
    private String inviteeEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AttendeeInviteStatus status = AttendeeInviteStatus.PENDING;

    /**
     * SHA-256 hash of the invite token (never store raw token).
     * Only set when the invite is created for an email flow.
     */
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
}
