package eventplanner.features.event.service;

import eventplanner.common.communication.enums.CommunicationType;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.services.core.dto.NotificationResponse;
import eventplanner.common.config.ExternalServicesProperties;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventReminder;
import eventplanner.features.event.enums.EmailTemplateType;
import eventplanner.features.event.repository.EventReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Polls the database every minute for reminders whose time has come and dispatches them.
 *
 * <p><strong>Transaction model:</strong> the scheduler method itself runs without a transaction
 * so that each reminder is processed in its own isolated transaction via {@link #dispatchReminder}.
 * This prevents a single bad reminder from rolling back the {@code wasSent=true} state of all
 * previously committed reminders in the same batch.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventReminderSchedulerService {

    private final EventReminderRepository reminderRepository;
    private final NotificationService notificationService;
    private final EventEmailTemplateService emailTemplateService;
    private final EventTemplateVariableService templateVariableService;
    private final ExternalServicesProperties externalServicesProperties;

    /**
     * Runs at the top of every minute (cron: second=0, all others=wildcard).
     * Loads pending reminders once, then processes each one in its own transaction.
     */
    @Scheduled(cron = "0 * * * * *")
    public void sendPendingReminders() {
        List<EventReminder> pending;
        try {
            pending = reminderRepository.findPendingRemindersToSend(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to query pending reminders", e);
            return;
        }

        if (pending.isEmpty()) {
            return;
        }

        log.info("Processing {} pending reminder(s)", pending.size());
        int successCount = 0;
        int failureCount = 0;

        for (EventReminder reminder : pending) {
            try {
                dispatchReminder(reminder.getId());
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to dispatch reminder {} — will retry next run: {}",
                        reminder.getId(), e.getMessage(), e);
            }
        }

        log.info("Reminder run complete: {} sent, {} failed", successCount, failureCount);
    }

    /**
     * Loads the reminder fresh inside its own transaction, sends all notifications,
     * and marks {@code wasSent = true} only after every send succeeds.
     *
     * <p>Isolated per reminder so one failure does not roll back another's commit.</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatchReminder(UUID reminderId) {
        EventReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new IllegalStateException("Reminder not found: " + reminderId));

        // Guard: skip if already sent or deactivated (race between poll and dispatch)
        if (Boolean.TRUE.equals(reminder.getWasSent()) || !Boolean.TRUE.equals(reminder.getIsActive())) {
            return;
        }

        if (reminder.getEvent() == null) {
            throw new IllegalStateException("Reminder " + reminderId + " has no associated event");
        }

        Event event = reminder.getEvent();
        CommunicationType channel = resolveChannel(reminder.getChannel());
        String subject = reminder.getTitle();
        String content = resolveContent(reminder);
        EmailTemplateType templateType = resolveTemplateType(reminder);

        Map<String, Object> templateVariables = templateVariableService.prepareTemplateVariables(
                event, content, subject, templateType);

        String templateId = resolveTemplateId(channel, templateType);

        // EMAIL: send to email addresses only
        if (channel == CommunicationType.EMAIL) {
            sendToEmails(reminder, channel, subject, templateId, templateVariables, event);
        }

        // PUSH: send to user IDs only (device tokens are resolved inside PushNotificationService)
        if (channel == CommunicationType.PUSH_NOTIFICATION) {
            sendToUserIds(reminder, channel, subject, content, templateVariables, event);
        }

        reminder.setWasSent(true);
        reminderRepository.save(reminder);
        log.info("Reminder {} dispatched successfully for event {}", reminderId, event.getId());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void sendToEmails(EventReminder reminder, CommunicationType channel,
                              String subject, String templateId,
                              Map<String, Object> templateVariables, Event event) {
        String csv = reminder.getRecipientEmailsCsv();
        if (csv == null || csv.isBlank()) return;

        for (String raw : csv.split(",")) {
            String email = raw.trim();
            if (email.isBlank()) continue;

            NotificationRequest req = NotificationRequest.builder()
                    .type(channel)
                    .to(email)
                    .subject(subject)
                    .templateId(templateId)
                    .templateVariables(new HashMap<>(templateVariables))
                    .eventId(event.getId())
                    .from(externalServicesProperties.getEmail().getFromEvents())
                    .build();

            NotificationResponse resp = notificationService.send(req);
            if (!resp.isSuccess()) {
                throw new IllegalStateException(
                        "Email send failed for reminder " + reminder.getId()
                        + " to " + email + ": " + resp.getErrorMessage());
            }
            log.debug("Reminder email sent to {}", email);
        }
    }

    private void sendToUserIds(EventReminder reminder, CommunicationType channel,
                               String subject, String content,
                               Map<String, Object> templateVariables, Event event) {
        String csv = reminder.getRecipientUserIdsCsv();
        if (csv == null || csv.isBlank()) return;

        for (String raw : csv.split(",")) {
            String userIdStr = raw.trim();
            if (userIdStr.isBlank()) continue;

            // Validate UUID before attempting the send so bad data doesn't surface as a push failure
            try {
                UUID.fromString(userIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("Reminder {} contains invalid user ID '{}', skipping", reminder.getId(), userIdStr);
                continue;
            }

            Map<String, Object> pushData = new HashMap<>(templateVariables);
            pushData.put("body", content);

            NotificationRequest req = NotificationRequest.builder()
                    .type(channel)
                    .to(userIdStr)
                    .subject(subject)
                    .templateVariables(pushData)
                    .eventId(event.getId())
                    .from(externalServicesProperties.getEmail().getFromEvents())
                    .build();

            NotificationResponse resp = notificationService.send(req);
            if (!resp.isSuccess()) {
                throw new IllegalStateException(
                        "Push send failed for reminder " + reminder.getId()
                        + " to user " + userIdStr + ": " + resp.getErrorMessage());
            }
            log.debug("Reminder push sent to user {}", userIdStr);
        }
    }

    private String resolveContent(EventReminder reminder) {
        if (reminder.getCustomMessage() != null && !reminder.getCustomMessage().isBlank()) {
            return reminder.getCustomMessage();
        }
        if (reminder.getDescription() != null && !reminder.getDescription().isBlank()) {
            return reminder.getDescription();
        }
        return reminder.getTitle();
    }

    private EmailTemplateType resolveTemplateType(EventReminder reminder) {
        if (reminder.getEmailTemplateType() == null) return null;
        try {
            return EmailTemplateType.valueOf(reminder.getEmailTemplateType());
        } catch (IllegalArgumentException e) {
            log.warn("Reminder {} has unknown emailTemplateType '{}', falling back to EVENT_REMINDER",
                    reminder.getId(), reminder.getEmailTemplateType());
            return EmailTemplateType.EVENT_REMINDER;
        }
    }

    /**
     * Resolves the Resend template ID to use.
     * Falls back to {@code event-reminder} when no explicit type is stored on the reminder.
     */
    private String resolveTemplateId(CommunicationType channel, EmailTemplateType templateType) {
        if (channel != CommunicationType.EMAIL) return null;
        EmailTemplateType effective = templateType != null ? templateType : EmailTemplateType.EVENT_REMINDER;
        return emailTemplateService.getTemplateId(effective);
    }

    private CommunicationType resolveChannel(String channel) {
        if (channel == null) return CommunicationType.EMAIL;
        return switch (channel.toLowerCase().trim()) {
            case "push", "push_notification" -> CommunicationType.PUSH_NOTIFICATION;
            case "sms"                        -> CommunicationType.SMS;
            default                           -> CommunicationType.EMAIL;
        };
    }
}
