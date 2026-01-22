package eventplanner.security.auth.repository;

import eventplanner.security.auth.entity.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserNotificationPreference entity.
 */
@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, UUID> {

    /**
     * Find all notification preferences for a user.
     */
    @Query("SELECT unp FROM UserNotificationPreference unp WHERE unp.user.id = :userId")
    List<UserNotificationPreference> findByUserId(@Param("userId") UUID userId);

    /**
     * Find preferences for a specific notification type.
     */
    @Query("SELECT unp FROM UserNotificationPreference unp WHERE unp.user.id = :userId AND unp.notificationType = :type")
    List<UserNotificationPreference> findByUserIdAndType(@Param("userId") UUID userId, @Param("type") String type);

    /**
     * Find a specific preference by user, type, and channel.
     */
    @Query("SELECT unp FROM UserNotificationPreference unp WHERE unp.user.id = :userId AND unp.notificationType = :type AND unp.channel = :channel")
    Optional<UserNotificationPreference> findByUserIdAndTypeAndChannel(
        @Param("userId") UUID userId,
        @Param("type") String type,
        @Param("channel") String channel
    );

    /**
     * Check if a user has enabled notifications for a specific type and channel.
     */
    @Query("SELECT unp.enabled FROM UserNotificationPreference unp WHERE unp.user.id = :userId AND unp.notificationType = :type AND unp.channel = :channel")
    Optional<Boolean> isEnabled(@Param("userId") UUID userId, @Param("type") String type, @Param("channel") String channel);
}
