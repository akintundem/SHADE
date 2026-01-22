package eventplanner.security.auth.repository;

import eventplanner.security.auth.entity.UserPrivacySetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserPrivacySetting entity.
 * Provides data access methods for user privacy settings.
 */
@Repository
public interface UserPrivacySettingRepository extends JpaRepository<UserPrivacySetting, UUID> {

    /**
     * Find all privacy settings for a user
     */
    @Query("SELECT ups FROM UserPrivacySetting ups WHERE ups.user.id = :userId")
    List<UserPrivacySetting> findByUserId(@Param("userId") UUID userId);

    /**
     * Find a specific privacy setting by user ID and setting key
     */
    @Query("SELECT ups FROM UserPrivacySetting ups WHERE ups.user.id = :userId AND ups.settingKey = :key")
    Optional<UserPrivacySetting> findByUserIdAndKey(
        @Param("userId") UUID userId,
        @Param("key") String key
    );

    /**
     * Check if a privacy setting exists for a user
     */
    @Query("SELECT COUNT(ups) > 0 FROM UserPrivacySetting ups WHERE ups.user.id = :userId AND ups.settingKey = :key")
    boolean existsByUserIdAndKey(@Param("userId") UUID userId, @Param("key") String key);

    /**
     * Delete all privacy settings for a user
     */
    @Query("DELETE FROM UserPrivacySetting ups WHERE ups.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
