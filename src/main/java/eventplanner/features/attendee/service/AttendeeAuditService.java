package eventplanner.features.attendee.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eventplanner.features.attendee.entity.AttendeeAuditLog;
import eventplanner.features.attendee.repository.AttendeeAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for logging attendee operations to audit trail
 * Provides comprehensive audit logging for compliance and tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttendeeAuditService {
    
    private final AttendeeAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Log an attendee action asynchronously to avoid blocking the main request flow
     */
    @Async
    @Transactional
    public void logAction(AttendeeAuditLog.ActionType actionType,
                         UUID attendeeId,
                         UUID eventId,
                         UUID performedBy,
                         String performedByEmail,
                         String ipAddress,
                         String userAgent,
                         String deviceId,
                         String oldValue,
                         String newValue,
                         String idempotencyKey,
                         Map<String, Object> metadata) {
        try {
            String metadataJson = null;
            if (metadata != null && !metadata.isEmpty()) {
                try {
                    metadataJson = objectMapper.writeValueAsString(metadata);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize metadata for audit log: {}", e.getMessage());
                }
            }
            
            AttendeeAuditLog auditLog = AttendeeAuditLog.builder()
                    .actionType(actionType)
                    .attendeeId(attendeeId)
                    .eventId(eventId)
                    .performedBy(performedBy)
                    .performedByEmail(performedByEmail)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .deviceId(deviceId)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .idempotencyKey(idempotencyKey)
                    .metadata(metadataJson)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            auditLogRepository.save(auditLog);
            log.debug("Attendee audit log saved: {} for attendee {} in event {}", actionType, attendeeId, eventId);
            
        } catch (Exception e) {
            // Never fail the main request due to audit logging issues
            log.error("Failed to save attendee audit log: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Log attendee creation
     */
    public void logAttendeeCreated(UUID attendeeId, UUID eventId, UUID performedBy, String performedByEmail,
                                   String ipAddress, String userAgent, String deviceId, Map<String, Object> metadata) {
        logAction(AttendeeAuditLog.ActionType.ATTENDEE_CREATED, attendeeId, eventId, performedBy, performedByEmail,
                ipAddress, userAgent, deviceId, null, null, null, metadata);
    }
    
    /**
     * Log attendee update
     */
    public void logAttendeeUpdated(UUID attendeeId, UUID eventId, UUID performedBy, String performedByEmail,
                                   String ipAddress, String userAgent, String oldValue, String newValue,
                                   Map<String, Object> metadata) {
        logAction(AttendeeAuditLog.ActionType.ATTENDEE_UPDATED, attendeeId, eventId, performedBy, performedByEmail,
                ipAddress, userAgent, null, oldValue, newValue, null, metadata);
    }
    
    /**
     * Log attendee deletion
     */
    public void logAttendeeDeleted(UUID attendeeId, UUID eventId, UUID performedBy, String performedByEmail,
                                   String ipAddress, String userAgent, Map<String, Object> metadata) {
        logAction(AttendeeAuditLog.ActionType.ATTENDEE_DELETED, attendeeId, eventId, performedBy, performedByEmail,
                ipAddress, userAgent, null, null, null, null, metadata);
    }
    
    /**
     * Log RSVP status update
     */
    public void logRsvpUpdated(UUID attendeeId, UUID eventId, UUID performedBy, String performedByEmail,
                              String ipAddress, String userAgent, String oldStatus, String newStatus,
                              Map<String, Object> metadata) {
        logAction(AttendeeAuditLog.ActionType.RSVP_UPDATED, attendeeId, eventId, performedBy, performedByEmail,
                ipAddress, userAgent, null, oldStatus, newStatus, null, metadata);
    }
    
    /**
     * Log check-in with idempotency support
     */
    public void logCheckIn(UUID attendeeId, UUID eventId, UUID performedBy, String performedByEmail,
                          String ipAddress, String userAgent, String deviceId, String idempotencyKey,
                          Map<String, Object> metadata) {
        logAction(AttendeeAuditLog.ActionType.CHECK_IN, attendeeId, eventId, performedBy, performedByEmail,
                ipAddress, userAgent, deviceId, null, null, idempotencyKey, metadata);
    }
    
    /**
     * Log invitation queued/sent with idempotency support
     */
    public void logInvitationQueued(UUID eventId, UUID performedBy, String performedByEmail,
                                    String ipAddress, String userAgent, String idempotencyKey,
                                    Map<String, Object> metadata) {
        logAction(AttendeeAuditLog.ActionType.INVITATION_QUEUED, null, eventId, performedBy, performedByEmail,
                ipAddress, userAgent, null, null, null, idempotencyKey, metadata);
    }
    
    /**
     * Log bulk import
     */
    public void logBulkImport(UUID eventId, UUID performedBy, String performedByEmail,
                             String ipAddress, String userAgent, int count, Map<String, Object> metadata) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("count", count);
        logAction(AttendeeAuditLog.ActionType.BULK_IMPORT, null, eventId, performedBy, performedByEmail,
                ipAddress, userAgent, null, null, null, null, metadata);
    }
    
    /**
     * Get audit trail for an attendee
     */
    @Transactional(readOnly = true)
    public List<AttendeeAuditLog> getAuditTrailForAttendee(UUID attendeeId) {
        return auditLogRepository.findByAttendeeIdOrderByTimestampDesc(attendeeId);
    }
    
    /**
     * Get audit trail for an event
     */
    @Transactional(readOnly = true)
    public List<AttendeeAuditLog> getAuditTrailForEvent(UUID eventId) {
        return auditLogRepository.findByEventIdOrderByTimestampDesc(eventId);
    }
}

