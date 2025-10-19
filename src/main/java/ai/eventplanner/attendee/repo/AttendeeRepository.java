package ai.eventplanner.attendee.repo;

import ai.eventplanner.attendee.model.AttendeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttendeeRepository extends JpaRepository<AttendeeEntity, UUID> {
    List<AttendeeEntity> findByEventId(UUID eventId);
}


