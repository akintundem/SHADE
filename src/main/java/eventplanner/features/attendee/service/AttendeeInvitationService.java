package eventplanner.features.attendee.service;

import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.features.attendee.entity.Attendee;
import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for asynchronously processing attendee invitations.
 * Handles bulk email and push notification sending in the background to avoid blocking HTTP requests.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttendeeInvitationService {

    private final NotificationService notificationService;
    private final AttendeeRepository attendeeRepository;
    private final UserAccountRepository userAccountRepository;
    
    // Batch size for processing notifications to avoid overwhelming services
    private static final int BATCH_SIZE = 50;
    
    /**
     * Asynchronously send invitations (email and/or push notifications) to a list of attendees.
     * Processes notifications in batches to avoid overwhelming the services.
     * 
     * @param eventId The event ID
     * @param attendeeIds List of attendee IDs to send invitations to
     * @param customMessage Optional custom message to include
     * @param sendEmail Whether to send email invitations
     * @param sendPush Whether to send push notifications
     * @return CompletableFuture with processing results
     */
    @Async
    public CompletableFuture<InvitationProcessingResult> sendInvitationsAsync(
            UUID eventId,
            List<UUID> attendeeIds,
            String customMessage,
            boolean sendEmail,
            boolean sendPush) {
        
        log.info("Starting async invitation processing for event {} with {} attendees (email: {}, push: {})", 
                eventId, attendeeIds.size(), sendEmail, sendPush);
        
        if (!sendEmail && !sendPush) {
            log.warn("Neither email nor push notifications requested for event {}", eventId);
            return CompletableFuture.completedFuture(
                    new InvitationProcessingResult(0, 0, new ArrayList<>(), new ArrayList<>()));
        }
        
        List<UUID> queuedAttendeeIds = new ArrayList<>();
        List<UUID> failedAttendeeIds = new ArrayList<>();
        
        // Fetch all attendees
        List<Attendee> attendees = attendeeRepository.findAllById(attendeeIds);
        
        // Filter to only attendees for this event
        List<Attendee> eventAttendees = attendees.stream()
                .filter(a -> a.getEvent() != null && a.getEvent().getId().equals(eventId))
                .toList();
        
        if (eventAttendees.isEmpty()) {
            log.warn("No valid attendees found for event {}", eventId);
            return CompletableFuture.completedFuture(
                    new InvitationProcessingResult(0, 0, queuedAttendeeIds, failedAttendeeIds));
        }
        
        // Process in batches to avoid overwhelming the services
        int totalBatches = (int) Math.ceil((double) eventAttendees.size() / BATCH_SIZE);
        
        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int start = batchIndex * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, eventAttendees.size());
            List<Attendee> batch = eventAttendees.subList(start, end);
            
            log.debug("Processing batch {}/{} for event {} (attendees {}-{})", 
                    batchIndex + 1, totalBatches, eventId, start + 1, end);
            
            processBatch(batch, eventId, customMessage, sendEmail, sendPush, 
                    queuedAttendeeIds, failedAttendeeIds);
            
            // Small delay between batches to avoid rate limiting
            if (batchIndex < totalBatches - 1) {
                try {
                    Thread.sleep(100); // 100ms delay between batches
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted during batch processing");
                    break;
                }
            }
        }
        
        log.info("Completed async invitation processing for event {}: {} queued, {} failed", 
                eventId, queuedAttendeeIds.size(), failedAttendeeIds.size());
        
        return CompletableFuture.completedFuture(
                new InvitationProcessingResult(queuedAttendeeIds.size(), failedAttendeeIds.size(),
                        queuedAttendeeIds, failedAttendeeIds));
    }
    
    private void processBatch(List<Attendee> batch, UUID eventId, String customMessage,
                             boolean sendEmail, boolean sendPush,
                             List<UUID> queuedAttendeeIds, List<UUID> failedAttendeeIds) {
        for (Attendee attendee : batch) {
            boolean emailSuccess = false;
            boolean pushSuccess = false;
            boolean anyFailure = false;
            
            // Prepare common template variables
            Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("attendeeName", attendee.getName() != null ? attendee.getName() : "Guest");
            templateVariables.put("eventId", eventId.toString());
            if (customMessage != null && !customMessage.trim().isEmpty()) {
                templateVariables.put("customMessage", customMessage);
            }
            
            // Send email invitation if requested
            if (sendEmail && attendee.getEmail() != null && !attendee.getEmail().trim().isEmpty()) {
                try {
                    NotificationRequest emailRequest = NotificationRequest.builder()
                            .type(CommunicationType.EMAIL)
                            .to(attendee.getEmail())
                            .subject("You're Invited to an Event")
                            .templateId("event-invitation")
                            .templateVariables(templateVariables)
                            .eventId(eventId)
                            .build();
                    
                    var emailResponse = notificationService.send(emailRequest);
                    emailSuccess = emailResponse.isSuccess();
                    
                    if (!emailSuccess) {
                        log.warn("Failed to send email invitation to attendee {}: {}", 
                                attendee.getId(), emailResponse.getErrorMessage());
                        anyFailure = true;
                    }
                } catch (Exception e) {
                    log.error("Error sending email invitation to attendee {}: {}", 
                            attendee.getId(), e.getMessage(), e);
                    anyFailure = true;
                }
            }
            
            // Send push notification if requested
            if (sendPush && attendee.getEmail() != null && !attendee.getEmail().trim().isEmpty()) {
                try {
                    // Look up user by email to get userId for push notification
                    UserAccount user = userAccountRepository.findByEmailIgnoreCase(attendee.getEmail().trim())
                            .orElse(null);
                    
                    if (user == null) {
                        log.debug("User not registered for email {} - skipping push notification", 
                                attendee.getEmail());
                        // Not a failure, just skip push notification for this attendee
                    } else {
                        // Prepare push notification data
                        Map<String, Object> pushTemplateVars = new HashMap<>(templateVariables);
                        String pushBody = customMessage != null && !customMessage.trim().isEmpty()
                                ? customMessage
                                : "You're invited to an event!";
                        pushTemplateVars.put("body", pushBody);
                        
                        NotificationRequest pushRequest = NotificationRequest.builder()
                                .type(CommunicationType.PUSH_NOTIFICATION)
                                .to(user.getId().toString()) // userId as string for push notifications
                                .subject("You're Invited to an Event")
                                .templateId(null) // Not needed for push notifications
                                .templateVariables(pushTemplateVars)
                                .eventId(eventId)
                                .build();
                        
                        var pushResponse = notificationService.send(pushRequest);
                        pushSuccess = pushResponse.isSuccess();
                        
                        if (!pushSuccess) {
                            log.warn("Failed to send push notification to attendee {} (user {}): {}", 
                                    attendee.getId(), user.getId(), pushResponse.getErrorMessage());
                            anyFailure = true;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error sending push notification to attendee {}: {}", 
                            attendee.getId(), e.getMessage(), e);
                    anyFailure = true;
                }
            }
            
            // Track success/failure
            // Consider it successful if at least one channel succeeded
            // Consider it failed only if all requested channels failed
            boolean overallSuccess = false;
            if (sendEmail && sendPush) {
                // Both requested - success if at least one succeeded
                overallSuccess = emailSuccess || pushSuccess;
            } else if (sendEmail) {
                // Only email requested
                overallSuccess = emailSuccess;
            } else if (sendPush) {
                // Only push requested
                overallSuccess = pushSuccess;
            }
            
            if (overallSuccess) {
                queuedAttendeeIds.add(attendee.getId());
            } else if (anyFailure || (!sendEmail && !sendPush)) {
                failedAttendeeIds.add(attendee.getId());
            }
        }
    }
    
    /**
     * Result of invitation processing
     */
    public record InvitationProcessingResult(
            int queuedCount,
            int failedCount,
            List<UUID> queuedAttendeeIds,
            List<UUID> failedAttendeeIds
    ) {}
}

