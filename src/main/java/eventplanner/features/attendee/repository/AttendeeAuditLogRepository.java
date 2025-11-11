package eventplanner.features.attendee.repository;

import eventplanner.features.attendee.entity.AttendeeAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendeeAuditLogRepository extends JpaRepository<AttendeeAuditLog, UUID> {
    
    List<AttendeeAuditLog> findByAttendeeIdOrderByTimestampDesc(UUID attendeeId);
    
    List<AttendeeAuditLog> findByEventIdOrderByTimestampDesc(UUID eventId);
    
    List<AttendeeAuditLog> findByPerformedByOrderByTimestampDesc(UUID userId);
    
    List<AttendeeAuditLog> findByActionTypeOrderByTimestampDesc(AttendeeAuditLog.ActionType actionType);
    
    Optional<AttendeeAuditLog> findByIdempotencyKey(String idempotencyKey);
    
    @Query("SELECT a FROM AttendeeAuditLog a WHERE a.eventId = :eventId AND a.actionType = :actionType ORDER BY a.timestamp DESC")
    List<AttendeeAuditLog> findByEventIdAndActionType(@Param("eventId") UUID eventId, @Param("actionType") AttendeeAuditLog.ActionType actionType);
    
    @Query("SELECT a FROM AttendeeAuditLog a WHERE a.attendeeId = :attendeeId AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AttendeeAuditLog> findRecentByAttendeeId(@Param("attendeeId") UUID attendeeId, @Param("since") LocalDateTime since);
}

