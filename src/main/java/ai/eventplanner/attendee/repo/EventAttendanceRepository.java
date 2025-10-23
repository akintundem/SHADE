package ai.eventplanner.attendee.repo;

import ai.eventplanner.attendee.entity.EventAttendance;
import ai.eventplanner.common.domain.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventAttendanceRepository extends JpaRepository<EventAttendance, UUID> {
    
    List<EventAttendance> findByEventId(UUID eventId);
    
    List<EventAttendance> findByEventIdAndAttendanceStatus(UUID eventId, AttendanceStatus status);
    
    Optional<EventAttendance> findByEventIdAndUserId(UUID eventId, UUID userId);
    
    Optional<EventAttendance> findByEventIdAndQrCode(UUID eventId, String qrCode);
    
    List<EventAttendance> findByEventIdAndQrCodeUsed(UUID eventId, Boolean qrCodeUsed);
    
    @Query("SELECT ea FROM EventAttendance ea WHERE ea.eventId = :eventId AND ea.attendanceStatus IN :statuses")
    List<EventAttendance> findByEventIdAndAttendanceStatusIn(@Param("eventId") UUID eventId, @Param("statuses") List<AttendanceStatus> statuses);
    
    @Query("SELECT COUNT(ea) FROM EventAttendance ea WHERE ea.eventId = :eventId AND ea.attendanceStatus = :status")
    Long countByEventIdAndAttendanceStatus(@Param("eventId") UUID eventId, @Param("status") AttendanceStatus status);
    
    @Query("SELECT ea FROM EventAttendance ea WHERE ea.eventId = :eventId AND ea.checkInTime IS NOT NULL ORDER BY ea.checkInTime ASC")
    List<EventAttendance> findCheckedInByEventIdOrderByCheckInTime(@Param("eventId") UUID eventId);
}
