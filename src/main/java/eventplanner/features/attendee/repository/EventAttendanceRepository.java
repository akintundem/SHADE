package eventplanner.features.attendee.repository;

import eventplanner.features.attendee.entity.EventAttendance;
import eventplanner.common.domain.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventAttendanceRepository extends JpaRepository<EventAttendance, UUID> {
    
    @Query("SELECT ea FROM EventAttendance ea WHERE ea.event.id = :eventId")
    List<EventAttendance> findByEventId(@Param("eventId") UUID eventId);
    
    @Query("SELECT ea FROM EventAttendance ea WHERE ea.event.id = :eventId AND ea.attendanceStatus = :status")
    List<EventAttendance> findByEventIdAndAttendanceStatus(@Param("eventId") UUID eventId, @Param("status") AttendanceStatus status);
    
    @Query("SELECT ea FROM EventAttendance ea WHERE ea.event.id = :eventId AND ea.user.id = :userId")
    Optional<EventAttendance> findByEventIdAndUserId(@Param("eventId") UUID eventId, @Param("userId") UUID userId);
    
    @Query("SELECT ea FROM EventAttendance ea WHERE ea.event.id = :eventId AND ea.qrCode = :qrCode")
    Optional<EventAttendance> findByEventIdAndQrCode(@Param("eventId") UUID eventId, @Param("qrCode") String qrCode);
    
    @Query("SELECT ea FROM EventAttendance ea WHERE ea.event.id = :eventId AND ea.qrCodeUsed = :qrCodeUsed")
    List<EventAttendance> findByEventIdAndQrCodeUsed(@Param("eventId") UUID eventId, @Param("qrCodeUsed") Boolean qrCodeUsed);
    
    @Query("SELECT ea FROM EventAttendance ea WHERE ea.event.id = :eventId AND ea.attendanceStatus IN :statuses")
    List<EventAttendance> findByEventIdAndAttendanceStatusIn(@Param("eventId") UUID eventId, @Param("statuses") List<AttendanceStatus> statuses);
    
    @Query("SELECT COUNT(ea) FROM EventAttendance ea WHERE ea.event.id = :eventId AND ea.attendanceStatus = :status")
    Long countByEventIdAndAttendanceStatus(@Param("eventId") UUID eventId, @Param("status") AttendanceStatus status);
    
    @Query("SELECT ea FROM EventAttendance ea WHERE ea.event.id = :eventId AND ea.checkInTime IS NOT NULL ORDER BY ea.checkInTime ASC")
    List<EventAttendance> findCheckedInByEventIdOrderByCheckInTime(@Param("eventId") UUID eventId);
}
