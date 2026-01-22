package eventplanner.features.user.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * User privacy settings stored as key-value pairs.
 * Normalizes privacy_settings JSON from user_settings.
 */
@Entity
@Table(name = "user_privacy_settings",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_privacy_settings", columnNames = {"user_id", "setting_key"}),
    indexes = {
        @Index(name = "idx_user_privacy_settings_user", columnList = "user_id"),
        @Index(name = "idx_user_privacy_settings_key", columnList = "setting_key")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserPrivacySetting extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_privacy_settings_user"))
    private UserAccount user;

    /**
     * Privacy setting key (e.g., "profile_visibility", "email_visible", "show_events", "allow_messages")
     */
    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    /**
     * Setting value (PUBLIC, PRIVATE, FRIENDS_ONLY, etc. - depends on key)
     */
    @Column(name = "setting_value", nullable = false, length = 100)
    private String settingValue;

    /**
     * Optional description
     */
    @Column(name = "description", length = 255)
    private String description;
}
