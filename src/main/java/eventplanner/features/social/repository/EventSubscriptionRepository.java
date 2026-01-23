package eventplanner.features.social.repository;

import eventplanner.features.social.entity.EventSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventSubscriptionRepository extends JpaRepository<EventSubscription, UUID> {

    /**
     * Find subscription for a user and event
     */
    @Query("SELECT s FROM EventSubscription s WHERE s.user.id = :userId AND s.event.id = :eventId")
    Optional<EventSubscription> findByUserIdAndEventId(
            @Param("userId") UUID userId,
            @Param("eventId") UUID eventId
    );

    /**
     * Check if user is subscribed to an event
     */
    @Query("SELECT COUNT(s) > 0 FROM EventSubscription s WHERE s.user.id = :userId AND s.event.id = :eventId")
    boolean existsByUserIdAndEventId(
            @Param("userId") UUID userId,
            @Param("eventId") UUID eventId
    );

    /**
     * Get all events a user is subscribed to
     */
    @Query("SELECT s FROM EventSubscription s WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    Page<EventSubscription> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Get all subscribers for an event
     */
    @Query("SELECT s FROM EventSubscription s WHERE s.event.id = :eventId ORDER BY s.createdAt DESC")
    Page<EventSubscription> findByEventId(@Param("eventId") UUID eventId, Pageable pageable);

    /**
     * Count subscribers for an event
     */
    @Query("SELECT COUNT(s) FROM EventSubscription s WHERE s.event.id = :eventId")
    long countByEventId(@Param("eventId") UUID eventId);

    /**
     * Get all event IDs a user is subscribed to
     */
    @Query("SELECT s.event.id FROM EventSubscription s WHERE s.user.id = :userId")
    List<UUID> findEventIdsByUserId(@Param("userId") UUID userId);

    /**
     * Delete subscription
     */
    @Query("DELETE FROM EventSubscription s WHERE s.user.id = :userId AND s.event.id = :eventId")
    void deleteByUserIdAndEventId(
            @Param("userId") UUID userId,
            @Param("eventId") UUID eventId
    );

    /**
     * Find subscriptions by event and subscription types (for notification filtering)
     */
    @Query("SELECT s FROM EventSubscription s WHERE s.event.id = :eventId AND s.subscriptionType IN :types")
    List<EventSubscription> findByEventIdAndSubscriptionTypeIn(
            @Param("eventId") UUID eventId,
            @Param("types") List<EventSubscription.SubscriptionType> types
    );

    /**
     * Find subscriptions by event and user IDs (batch check)
     */
    @Query("SELECT s FROM EventSubscription s WHERE s.event.id = :eventId AND s.user.id IN :userIds")
    List<EventSubscription> findByEventIdAndUserIdIn(
            @Param("eventId") UUID eventId,
            @Param("userIds") List<UUID> userIds
    );
}
