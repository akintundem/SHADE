package eventplanner.features.attendee.service;

import eventplanner.features.attendee.dto.request.SendInvitationRequest;
import eventplanner.features.attendee.dto.response.InvitationResponse;
import eventplanner.features.attendee.entity.EventAttendance;
import eventplanner.features.attendee.repository.EventAttendanceRepository;
import eventplanner.common.communication.model.Communication;
import eventplanner.common.communication.repository.CommunicationRepository;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.services.core.dto.NotificationResponse;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.common.domain.enums.CommunicationStatus;
import eventplanner.common.util.EventValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling attendee communications (emails, notifications, invitations).
 * All methods validate event existence and ensure proper authorization context.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AttendeeCommunicationService {
    
    private final EventAttendanceRepository attendanceRepository;
    private final NotificationService notificationService;
    private final CommunicationRepository communicationRepository;
    private final EventValidationUtil eventValidationUtil;
    
    /**
     * Send bulk email to all attendees of an event.
     * Authorization: Enforced at controller level via RBAC (communication.send permission)
     * 
     * @param eventId The event ID
     * @param request The email request with subject and custom message
     * @return Map with sent, failed, total counts and status
     */
    public Map<String, Object> sendBulkEmail(UUID eventId, SendInvitationRequest request) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Email subject is required");
        }
        
        eventValidationUtil.validateEventExists(eventId);
        
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        long sent = 0;
        long failed = 0;
        
        for (EventAttendance attendance : attendances) {
            if (attendance.getEmail() == null || attendance.getEmail().trim().isEmpty()) {
                failed++;
                continue;
            }
            
            try {
                Map<String, Object> templateVariables = buildTemplateVariables(attendance, eventId, request);
                
                NotificationRequest notificationRequest = NotificationRequest.builder()
                        .type(CommunicationType.EMAIL)
                        .to(attendance.getEmail())
                        .subject(request.getSubject())
                        .templateId("event-invitation")
                        .templateVariables(templateVariables)
                        .eventId(eventId)
                        .build();
                
                NotificationResponse response = notificationService.send(notificationRequest);
                if (response.isSuccess()) {
                    sent++;
                } else {
                    failed++;
                    log.warn("Failed to send email to {} for event {}: {}", 
                            attendance.getEmail(), eventId, response.getErrorMessage());
                }
            } catch (Exception e) {
                failed++;
                log.error("Exception sending email to {} for event {}: {}", 
                        attendance.getEmail(), eventId, e.getMessage(), e);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("sent", sent);
        result.put("failed", failed);
        result.put("total", attendances.size());
        result.put("status", sent > 0 ? "completed" : "failed");
        
        return result;
    }
    
    /**
     * Send notification to a specific attendee.
     * Authorization: Enforced at controller level via RBAC (communication.send permission)
     * 
     * @param eventId The event ID
     * @param attendanceId The attendance ID
     * @param request The notification request
     * @return Map with success status and communication details
     */
    public Map<String, Object> sendNotification(UUID eventId, UUID attendanceId, SendInvitationRequest request) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (attendanceId == null) {
            throw new IllegalArgumentException("Attendance ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Email subject is required");
        }
        
        eventValidationUtil.validateEventExists(eventId);
        
        EventAttendance attendance = attendanceRepository.findById(attendanceId)
                .filter(a -> a.getEvent() != null && a.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new RuntimeException("Attendance not found for event: " + eventId));
        
        if (attendance.getEmail() == null || attendance.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Attendee email is required for notifications");
        }
        
        try {
            Map<String, Object> templateVariables = buildTemplateVariables(attendance, eventId, request);
            
            NotificationRequest notificationRequest = NotificationRequest.builder()
                    .type(CommunicationType.EMAIL)
                    .to(attendance.getEmail())
                    .subject(request.getSubject())
                    .templateId("event-invitation")
                    .templateVariables(templateVariables)
                    .eventId(eventId)
                    .build();
            
            NotificationResponse response = notificationService.send(notificationRequest);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("communicationId", response.getCommunicationId());
            result.put("messageId", response.getMessageId());
            result.put("status", response.getStatus());
            if (!response.isSuccess()) {
                result.put("errorMessage", response.getErrorMessage());
            }
            
            return result;
        } catch (Exception e) {
            log.error("Failed to send notification to attendee {} for event {}: {}", 
                    attendanceId, eventId, e.getMessage(), e);
            throw new RuntimeException("Failed to send notification: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send invitations to all attendees of an event.
     * Authorization: Enforced at controller level via RBAC (communication.send permission)
     * 
     * @param eventId The event ID
     * @param request The invitation request
     * @return InvitationResponse with details of sent invitations
     */
    public InvitationResponse sendInvitations(UUID eventId, SendInvitationRequest request) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("Email subject is required");
        }
        
        eventValidationUtil.validateEventExists(eventId);
        
        List<EventAttendance> attendances = attendanceRepository.findByEventId(eventId);
        List<InvitationResponse.InvitationDetail> invitationDetails = new ArrayList<>();
        long totalSent = 0;
        long totalDelivered = 0;
        long totalFailed = 0;
        
        for (EventAttendance attendance : attendances) {
            if (attendance.getEmail() == null || attendance.getEmail().trim().isEmpty()) {
                totalFailed++;
                invitationDetails.add(new InvitationResponse.InvitationDetail(
                        attendance.getEmail(),
                        "failed",
                        "No email address",
                        null,
                        null,
                        "No email address provided"
                ));
                continue;
            }
            
            try {
                Map<String, Object> templateVariables = buildTemplateVariables(attendance, eventId, request);
                
                NotificationRequest notificationRequest = NotificationRequest.builder()
                        .type(CommunicationType.EMAIL)
                        .to(attendance.getEmail())
                        .subject(request.getSubject())
                        .templateId("event-invitation")
                        .templateVariables(templateVariables)
                        .eventId(eventId)
                        .build();
                
                NotificationResponse response = notificationService.send(notificationRequest);
                
                if (response.isSuccess()) {
                    totalSent++;
                    invitationDetails.add(new InvitationResponse.InvitationDetail(
                            attendance.getEmail(),
                            "sent",
                            "Invitation sent successfully",
                            LocalDateTime.now(),
                            null,
                            null
                    ));
                } else {
                    totalFailed++;
                    invitationDetails.add(new InvitationResponse.InvitationDetail(
                            attendance.getEmail(),
                            "failed",
                            "Failed to send invitation",
                            null,
                            null,
                            response.getErrorMessage()
                    ));
                }
            } catch (Exception e) {
                totalFailed++;
                invitationDetails.add(new InvitationResponse.InvitationDetail(
                        attendance.getEmail(),
                        "failed",
                        "Exception occurred",
                        null,
                        null,
                        e.getMessage()
                ));
                log.error("Exception sending invitation to {} for event {}: {}", 
                        attendance.getEmail(), eventId, e.getMessage(), e);
            }
        }
        
        return new InvitationResponse(
                eventId,
                invitationDetails,
                totalSent,
                totalDelivered,
                totalFailed,
                LocalDateTime.now(),
                totalSent > 0 ? "completed" : "failed"
        );
    }
    
    /**
     * Get communication history for an event.
     * Authorization: Enforced at controller level via RBAC (communication.read permission)
     * 
     * @param eventId The event ID
     * @return List of communication records
     */
    public List<Map<String, Object>> getCommunicationHistory(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        List<Communication> communications = communicationRepository.findByEventIdOrderByCreatedAtDesc(eventId);
        return communications.stream()
                .map(comm -> {
                    Map<String, Object> commMap = new HashMap<>();
                    commMap.put("id", comm.getId());
                    commMap.put("type", comm.getCommunicationType());
                    commMap.put("recipientEmail", comm.getRecipientEmail());
                    commMap.put("subject", comm.getSubject());
                    commMap.put("status", comm.getStatus());
                    commMap.put("sentAt", comm.getSentAt());
                    commMap.put("deliveredAt", comm.getDeliveredAt());
                    commMap.put("createdAt", comm.getCreatedAt());
                    return commMap;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Get sent invitations for an event.
     * Authorization: Enforced at controller level via RBAC (communication.read permission)
     * 
     * @param eventId The event ID
     * @return List of invitation responses grouped by date
     */
    public List<InvitationResponse> getSentInvitations(UUID eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Event ID cannot be null");
        }
        eventValidationUtil.validateEventExists(eventId);
        
        List<Communication> communications = communicationRepository.findByEventIdOrderByCreatedAtDesc(eventId);
        Map<LocalDateTime, List<Communication>> groupedByDate = communications.stream()
                .filter(c -> c.getCommunicationType() == CommunicationType.EMAIL)
                .collect(Collectors.groupingBy(Communication::getSentAt));
        
        return groupedByDate.entrySet().stream()
                .map(entry -> {
                    List<Communication> comms = entry.getValue();
                    List<InvitationResponse.InvitationDetail> details = comms.stream()
                            .map(comm -> new InvitationResponse.InvitationDetail(
                                    comm.getRecipientEmail(),
                                    comm.getStatus().toString(),
                                    comm.getSubject(),
                                    comm.getSentAt(),
                                    comm.getDeliveredAt(),
                                    comm.getFailureReason()
                            ))
                            .collect(Collectors.toList());
                    
                    long sent = comms.stream().filter(c -> c.getStatus() == CommunicationStatus.SENT).count();
                    long delivered = comms.stream().filter(c -> c.getStatus() == CommunicationStatus.DELIVERED).count();
                    long failed = comms.stream().filter(c -> c.getStatus() == CommunicationStatus.FAILED).count();
                    
                    return new InvitationResponse(
                            eventId,
                            details,
                            sent,
                            delivered,
                            failed,
                            entry.getKey(),
                            "completed"
                    );
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Build template variables for email notifications
     */
    private Map<String, Object> buildTemplateVariables(EventAttendance attendance, UUID eventId, SendInvitationRequest request) {
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("attendeeName", attendance.getName() != null ? attendance.getName() : "Guest");
        templateVariables.put("eventId", eventId.toString());
        if (request.getCustomMessage() != null) {
            templateVariables.put("customMessage", request.getCustomMessage());
        }
        return templateVariables;
    }
}

