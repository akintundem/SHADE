package eventplanner.features.user.repository;

import eventplanner.features.user.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserPreference entity.
 */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    /**
     * Find all preferences for a user.
     */
    @Query("SELECT up FROM UserPreference up WHERE up.user.id = :userId")
    List<UserPreference> findByUserId(@Param("userId") UUID userId);

    /**
     * Find a specific preference by user and key.
     */
    @Query("SELECT up FROM UserPreference up WHERE up.user.id = :userId AND up.preferenceKey = :key")
    Optional<UserPreference> findByUserIdAndKey(@Param("userId") UUID userId, @Param("key") String key);

    /**
     * Check if a preference exists for a user.
     */
    @Query("SELECT COUNT(up) > 0 FROM UserPreference up WHERE up.user.id = :userId AND up.preferenceKey = :key")
    boolean existsByUserIdAndKey(@Param("userId") UUID userId, @Param("key") String key);

    /**
     * Delete a specific preference.
     */
    @Query("DELETE FROM UserPreference up WHERE up.user.id = :userId AND up.preferenceKey = :key")
    void deleteByUserIdAndKey(@Param("userId") UUID userId, @Param("key") String key);
}
