package ai.eventplanner.event.repo;

import ai.eventplanner.event.entity.EventNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventNotificationSettingsRepository extends JpaRepository<EventNotificationSettings, UUID> {
    Optional<EventNotificationSettings> findByEventId(UUID eventId);
}
