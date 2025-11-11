package eventplanner.features.attendee.service;

import eventplanner.features.attendee.dto.response.CheckInResponse;
import eventplanner.features.attendee.entity.EventAttendance;
import eventplanner.features.attendee.repository.EventAttendanceRepository;
import eventplanner.common.domain.enums.AttendanceStatus;
import eventplanner.common.util.EventValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for QR code operations (generation, retrieval, scanning, regeneration).
 * All methods validate event existence and ensure proper authorization context.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AttendeeQRCodeService {
    
    private final EventAttendanceRepository attendanceRepository;
    private final EventValidationUtil eventValidationUtil;
    
    /**
     * Get QR code for a specific attendee.
     * Authorization: Enforced at controller level via RBAC (attendee.qrcode.read permission)
     * 
     * @param attendanceId The attendance ID
     * @return QR code string
     */
    public String getAttendeeQRCode(UUID attendanceId) {
        if (attendanceId == null) {
            throw new IllegalArgumentException("Attendance ID cannot be null");
        }
        
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found: " + attendanceId));
        
        eventValidationUtil.validateEventExists(attendance.getEventId());
        
        if (attendance.getQrCode() == null) {
            // Generate QR code if it doesn't exist
            String qrCode = generateQRCode(attendance.getEventId(), attendance.getUserId());
            attendance.setQrCode(qrCode);
            attendanceRepository.save(attendance);
            return qrCode;
        }
        
        return attendance.getQrCode();
    }
    
    /**
     * Regenerate QR code for an attendee.
     * Authorization: Enforced at controller level via RBAC (attendee.qrcode.regenerate permission)
     * 
     * @param attendanceId The attendance ID
     * @return New QR code string
     */
    public String regenerateQRCode(UUID attendanceId) {
        if (attendanceId == null) {
            throw new IllegalArgumentException("Attendance ID cannot be null");
        }
        
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance not found: " + attendanceId));
        
        eventValidationUtil.validateEventExists(attendance.getEventId());
        
        String newQrCode = generateQRCode(attendance.getEventId(), attendance.getUserId());
        attendance.setQrCode(newQrCode);
        attendance.setQrCodeUsed(false);
        attendance.setQrCodeUsedAt(null);
        attendanceRepository.save(attendance);
        
        log.info("Regenerated QR code for attendance {} in event {}", attendanceId, attendance.getEventId());
        
        return newQrCode;
    }
    
    /**
     * Scan QR code for check-in.
     * Authorization: Enforced at controller level via RBAC (attendee.qrcode.scan permission)
     * 
     * @param eventId The event ID
     * @param qrCode The QR code to scan
     * @return CheckInResponse with check-in details
     */
    public CheckInResponse scanQRCode(UUID eventId, String qrCode) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (qrCode == null || qrCode.trim().isEmpty()) {
            throw new IllegalArgumentException("QR code cannot be null or empty");
        }
        
        eventValidationUtil.validateEventExists(eventId);
        
        EventAttendance attendance = attendanceRepository.findByEventIdAndQrCode(eventId, qrCode)
                .orElseThrow(() -> new RuntimeException("Invalid QR code"));
        
        if (attendance.getQrCodeUsed()) {
            throw new RuntimeException("QR code already used");
        }
        
        // Perform check-in
        AttendanceStatus previousStatus = attendance.getAttendanceStatus();
        attendance.setAttendanceStatus(AttendanceStatus.CHECKED_IN);
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setQrCodeUsed(true);
        attendance.setQrCodeUsedAt(LocalDateTime.now());
        
        String existingNotes = attendance.getNotes() != null ? attendance.getNotes() : "";
        String checkInNote = "Checked in via QR code at " + LocalDateTime.now();
        attendance.setNotes(existingNotes.isEmpty() ? checkInNote : existingNotes + "\n" + checkInNote);
        
        EventAttendance saved = attendanceRepository.save(attendance);
        
        log.info("Scanned QR code for attendance {} in event {}", attendance.getId(), eventId);
        
        return new CheckInResponse(
                saved.getId(),
                saved.getEventId(),
                saved.getName(),
                saved.getEmail(),
                previousStatus,
                saved.getAttendanceStatus(),
                saved.getCheckInTime(),
                saved.getQrCode(),
                saved.getQrCodeUsed(),
                "Successfully checked in via QR code",
                "QR code scan"
        );
    }
    
    /**
     * Generate a unique QR code for an event and user.
     * 
     * @param eventId The event ID
     * @param userId The user ID
     * @return Generated QR code string
     */
    public String generateQRCode(UUID eventId, UUID userId) {
        if (eventId == null || userId == null) {
            throw new IllegalArgumentException("Event ID and User ID cannot be null");
        }
        
        // Generate a unique QR code: QR_<eventId_prefix>_<userId_prefix>_<timestamp>
        String eventPrefix = eventId.toString().substring(0, 8);
        String userPrefix = userId.toString().substring(0, 8);
        long timestamp = System.currentTimeMillis();
        
        return "QR_" + eventPrefix + "_" + userPrefix + "_" + timestamp;
    }
}

