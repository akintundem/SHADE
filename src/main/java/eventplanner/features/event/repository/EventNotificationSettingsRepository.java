package eventplanner.features.event.repository;

import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventNotificationSettingsRepository extends JpaRepository<EventNotificationSettings, UUID> {
    // UUID-based query (for backward compatibility)
    Optional<EventNotificationSettings> findByEventId(UUID eventId);
    
    // Relationship-based query
    Optional<EventNotificationSettings> findByEvent(Event event);
}
