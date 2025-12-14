package eventplanner.features.collaboration.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.domain.enums.EventUserType;
import eventplanner.features.collaboration.enums.CollaboratorInviteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_collaborator_invites",
        indexes = {
                @Index(name = "idx_collab_inv_event_id", columnList = "event_id"),
                @Index(name = "idx_collab_inv_invitee_user_id", columnList = "invitee_user_id"),
                @Index(name = "idx_collab_inv_invitee_email", columnList = "invitee_email"),
                @Index(name = "idx_collab_inv_status", columnList = "status"),
                @Index(name = "idx_collab_inv_token_hash", columnList = "token_hash")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventCollaboratorInvite extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "inviter_user_id", nullable = false)
    private UUID inviterUserId;

    @Column(name = "invitee_user_id")
    private UUID inviteeUserId;

    @Column(name = "invitee_email", length = 180)
    private String inviteeEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 40)
    private EventUserType role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CollaboratorInviteStatus status = CollaboratorInviteStatus.PENDING;

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

