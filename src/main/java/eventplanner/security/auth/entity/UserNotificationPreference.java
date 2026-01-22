package eventplanner.security.auth.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * User notification preferences - granular control per notification type and channel.
 * Normalizes notification_preferences JSON from user_settings.
 */
@Entity
@Table(name = "user_notification_preferences",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_notif_prefs",
        columnNames = {"user_id", "notification_type", "channel"}
    ),
    indexes = {
        @Index(name = "idx_user_notif_prefs_user", columnList = "user_id"),
        @Index(name = "idx_user_notif_prefs_type", columnList = "notification_type")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserNotificationPreference extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_notif_prefs_user"))
    private UserAccount user;

    /**
     * Type of notification (e.g., EVENT_REMINDER, TICKET_SOLD, NEW_POST, TASK_ASSIGNED, etc.)
     */
    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    /**
     * Communication channel (EMAIL, PUSH, SMS)
     */
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    /**
     * Whether this notification type is enabled for this channel
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * Optional frequency setting (IMMEDIATE, DAILY_DIGEST, WEEKLY_DIGEST)
     */
    @Column(name = "frequency", length = 20)
    private String frequency;
}
