package eventplanner.features.event.service;

import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.services.core.dto.NotificationResponse;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventReminder;
import eventplanner.features.event.enums.EmailTemplateType;
import eventplanner.features.event.repository.EventReminderRepository;
import eventplanner.features.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scheduled service to automatically send reminders when their time is up
 * Runs every minute to check for reminders that need to be sent
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventReminderSchedulerService {

    private final EventReminderRepository reminderRepository;
    private final NotificationService notificationService;
    private final EventRepository eventRepository;
    private final EventEmailTemplateService emailTemplateService;
    private final EventTemplateVariableService templateVariableService;

    /**
     * Scheduled task that runs every minute to check and send pending reminders
     * Cron expression: second, minute, hour, day, month, weekday
     * "0 * * * * *" means: at 0 seconds of every minute
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void sendPendingReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<EventReminder> pendingReminders = reminderRepository.findPendingRemindersToSend(now);

            if (pendingReminders.isEmpty()) {
                return;
            }

            log.info("Processing {} pending reminders", pendingReminders.size());

            int successCount = 0;
            int failureCount = 0;

            for (EventReminder reminder : pendingReminders) {
                try {
                    sendReminder(reminder);
                    reminder.setWasSent(true);
                    reminderRepository.save(reminder);
                    successCount++;
                    UUID eventId = reminder.getEvent() != null ? reminder.getEvent().getId() : null;
                    log.debug("Successfully sent reminder {} for event {}", reminder.getId(), eventId);
                } catch (Exception e) {
                    failureCount++;
                    UUID eventId = reminder.getEvent() != null ? reminder.getEvent().getId() : null;
                    log.error("Failed to send reminder {} for event {}: {}", 
                            reminder.getId(), eventId, e.getMessage(), e);
                    // Don't mark as sent if it failed - will retry on next run
                }
            }

            log.info("Reminder processing complete: {} successful, {} failed", successCount, failureCount);
        } catch (Exception e) {
            log.error("Error in scheduled reminder processing", e);
        }
    }

    /**
     * Send a reminder notification
     */
    private void sendReminder(EventReminder reminder) {
        CommunicationType communicationType = mapChannelToCommunicationType(reminder.getChannel());
        
        // Fetch event details for template variables
        if (reminder.getEvent() == null) {
            throw new IllegalStateException("Event not found for reminder: " + reminder.getId());
        }
        Event event = reminder.getEvent();
        
        // Prepare message content
        String subject = reminder.getTitle();
        String content = reminder.getCustomMessage() != null && !reminder.getCustomMessage().isBlank()
                ? reminder.getCustomMessage()
                : reminder.getDescription() != null ? reminder.getDescription() : reminder.getTitle();

        // Resolve email template type
        EmailTemplateType templateType = null;
        if (reminder.getEmailTemplateType() != null) {
            try {
                templateType = EmailTemplateType.valueOf(reminder.getEmailTemplateType());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid email template type in reminder: {}", reminder.getEmailTemplateType());
            }
        }

        // Prepare template variables with event details using shared service
        Map<String, Object> templateVariables = templateVariableService.prepareTemplateVariables(
            event, 
            content, 
            subject, 
            templateType
        );
        
        // Resolve template ID if email template type is specified
        String templateId = null;
        if (communicationType == CommunicationType.EMAIL && templateType != null) {
            templateId = emailTemplateService.getTemplateId(templateType);
            log.info("Using email template: {} for reminder {}", templateId, reminder.getId());
        }

        // Send to email recipients
        if (reminder.getRecipientEmailsCsv() != null && !reminder.getRecipientEmailsCsv().isBlank()) {
            String[] emails = reminder.getRecipientEmailsCsv().split(",");
            for (String email : emails) {
                email = email.trim();
                if (email.isBlank()) continue;

                try {
                    NotificationRequest notificationRequest = NotificationRequest.builder()
                            .type(communicationType)
                            .to(email)
                            .subject(subject)
                            .templateId(templateId)
                            .templateVariables(templateVariables)
                            .eventId(event.getId())
                            .build();

                    NotificationResponse response = notificationService.send(notificationRequest);
                    if (!response.isSuccess()) {
                        log.error("Failed to send reminder email to {}: {}", email, response.getErrorMessage());
                        throw new IllegalStateException("Email send failed: " + response.getErrorMessage());
                    }
                    log.debug("Successfully sent reminder email to {}", email);
                } catch (Exception e) {
                    log.error("Failed to send reminder email to {}: {}", email, e.getMessage());
                    throw e; // Re-throw to mark reminder as failed
                }
            }
        }

        // Send to user IDs (for push notifications)
        if (reminder.getRecipientUserIdsCsv() != null && !reminder.getRecipientUserIdsCsv().isBlank()) {
            String[] userIds = reminder.getRecipientUserIdsCsv().split(",");
            for (String userIdStr : userIds) {
                userIdStr = userIdStr.trim();
                if (userIdStr.isBlank()) continue;

                try {
                    Map<String, Object> pushData = new HashMap<>(templateVariables);
                    pushData.put("body", content);

                    NotificationRequest notificationRequest = NotificationRequest.builder()
                            .type(communicationType)
                            .to(userIdStr)
                            .subject(subject)
                            .templateVariables(pushData)
                            .eventId(event.getId())
                            .build();

                    NotificationResponse response = notificationService.send(notificationRequest);
                    if (!response.isSuccess()) {
                        log.error("Failed to send reminder push to user {}: {}", userIdStr, response.getErrorMessage());
                        throw new IllegalStateException("Push notification send failed: " + response.getErrorMessage());
                    }
                    log.debug("Successfully sent reminder push to user {}", userIdStr);
                } catch (Exception e) {
                    log.error("Failed to send reminder push to user {}: {}", userIdStr, e.getMessage());
                    throw e; // Re-throw to mark reminder as failed
                }
            }
        }
    }


    /**
     * Map reminder channel string to CommunicationType enum
     */
    private CommunicationType mapChannelToCommunicationType(String channel) {
        if (channel == null) {
            return CommunicationType.EMAIL; // Default
        }
        
        String channelLower = channel.toLowerCase().trim();
        return switch (channelLower) {
            case "email" -> CommunicationType.EMAIL;
            case "sms" -> CommunicationType.SMS;
            case "push", "push_notification" -> CommunicationType.PUSH_NOTIFICATION;
            default -> CommunicationType.EMAIL; // Default to email
        };
    }
}
