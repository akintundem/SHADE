package eventplanner.common.communication.repository;

import eventplanner.common.communication.model.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    
    /**
     * Find active device tokens for a user
     */
    List<DeviceToken> findByUserIdAndIsActiveTrue(UUID userId);
    
    /**
     * Find device token by token string
     */
    Optional<DeviceToken> findByDeviceToken(String deviceToken);
    
    /**
     * Find all active device tokens
     */
    List<DeviceToken> findByIsActiveTrue();
    
    /**
     * Find all inactive device tokens
     */
    List<DeviceToken> findByIsActiveFalse();
    
    /**
     * Deactivate old device tokens that haven't been used recently
     */
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.lastUsedAt < :cutoffDate AND dt.isActive = true")
    int deactivateOldTokens(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find device tokens for multiple users
     */
    @Query("SELECT dt FROM DeviceToken dt WHERE dt.userId IN :userIds AND dt.isActive = true")
    List<DeviceToken> findByUserIdInAndIsActiveTrue(@Param("userIds") List<UUID> userIds);
}
