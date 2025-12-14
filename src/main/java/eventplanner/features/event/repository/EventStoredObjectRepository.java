package eventplanner.features.event.repository;

import eventplanner.features.event.entity.EventStoredObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventStoredObjectRepository extends JpaRepository<EventStoredObject, UUID> {
    List<EventStoredObject> findByEventIdAndPurposeOrderByCreatedAtDesc(UUID eventId, String purpose);
}


