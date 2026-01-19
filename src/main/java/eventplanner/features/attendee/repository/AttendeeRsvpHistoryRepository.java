package eventplanner.features.attendee.repository;

import eventplanner.features.attendee.entity.AttendeeRsvpHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AttendeeRsvpHistoryRepository extends JpaRepository<AttendeeRsvpHistory, UUID> {

    Page<AttendeeRsvpHistory> findByEventId(UUID eventId, Pageable pageable);

    Page<AttendeeRsvpHistory> findByAttendeeId(UUID attendeeId, Pageable pageable);
}
