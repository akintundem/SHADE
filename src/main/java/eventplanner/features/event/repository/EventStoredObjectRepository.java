package eventplanner.features.event.repository;

import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventStoredObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventStoredObjectRepository extends JpaRepository<EventStoredObject, UUID> {
    // UUID-based query (for backward compatibility)
    @Query("SELECT o FROM EventStoredObject o WHERE o.event.id = :eventId AND o.purpose = :purpose ORDER BY o.createdAt DESC")
    List<EventStoredObject> findByEventIdAndPurposeOrderByCreatedAtDesc(@Param("eventId") UUID eventId, @Param("purpose") String purpose);
    
    // Relationship-based query
    List<EventStoredObject> findByEventAndPurposeOrderByCreatedAtDesc(Event event, String purpose);
}


