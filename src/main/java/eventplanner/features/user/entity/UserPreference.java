package eventplanner.features.user.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * User preferences stored as key-value pairs for 1NF compliance.
 * Replaces JSON preferences field in UserAccount.
 */
@Entity
@Table(name = "user_preferences",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_preferences", columnNames = {"user_id", "preference_key"}),
    indexes = {
        @Index(name = "idx_user_preferences_user", columnList = "user_id"),
        @Index(name = "idx_user_preferences_key", columnList = "preference_key")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserPreference extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_preferences_user"))
    private UserAccount user;

    /**
     * Preference key (e.g., "theme", "language", "timezone", "date_format")
     */
    @Column(name = "preference_key", nullable = false, length = 100)
    private String preferenceKey;

    /**
     * Preference value (stored as string, cast as needed)
     */
    @Column(name = "preference_value", length = 500)
    private String preferenceValue;

    /**
     * Optional description of what this preference controls
     */
    @Column(name = "description", length = 255)
    private String description;
}
