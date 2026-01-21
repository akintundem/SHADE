package eventplanner.features.social.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_follows",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_follows_follower_followee", columnNames = {"follower_id", "followee_id"})
    },
    indexes = {
        @Index(name = "idx_user_follows_follower", columnList = "follower_id"),
        @Index(name = "idx_user_follows_followee", columnList = "followee_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserFollow extends BaseEntity {

    public enum FollowStatus {
        /**
         * Follow is active (for public profiles or accepted requests)
         */
        ACTIVE,

        /**
         * Follow request is pending (for private profiles)
         */
        PENDING,

        /**
         * Follow was blocked by the followee
         */
        BLOCKED
    }

    /**
     * User who is following (the follower)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private UserAccount follower;

    /**
     * User being followed (the followee)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "followee_id", nullable = false)
    private UserAccount followee;

    /**
     * Status of the follow relationship
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FollowStatus status = FollowStatus.ACTIVE;
}
