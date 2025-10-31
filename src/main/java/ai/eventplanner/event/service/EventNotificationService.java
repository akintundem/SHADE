package ai.eventplanner.event.service;

import ai.eventplanner.comms.entity.Communication;
import ai.eventplanner.comms.service.NotificationService;
import ai.eventplanner.event.dto.request.EventNotificationRequest;
import ai.eventplanner.event.dto.response.EventNotificationResponse;
import ai.eventplanner.event.entity.EventNotificationSettings;
import ai.eventplanner.event.enums.EventNotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EventNotificationService {

    private final NotificationService notificationService;
    private final EventNotificationSettingsService settingsService;

    public EventNotificationResponse sendNotification(UUID eventId, EventNotificationRequest request) {
        EventNotificationSettings settings = settingsService.getSettingsEntity(eventId);
        validateChannelEnabled(settings, request.getChannel());

        Communication communication = notificationService.send(
            eventId,
            request.getChannel().toCommunicationType(),
            request.getSubject(),
            request.getContent(),
            request.getRecipientEmails(),
            request.getScheduledAt(),
            request.getPriority().name()
        );

        return toResponse(eventId, request, communication);
    }

    private void validateChannelEnabled(EventNotificationSettings settings, EventNotificationChannel channel) {
        if (channel == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notification channel is required");
        }
        boolean enabled = switch (channel) {
            case EMAIL -> Boolean.TRUE.equals(settings.getEmailEnabled());
            case SMS -> Boolean.TRUE.equals(settings.getSmsEnabled());
            case PUSH -> Boolean.TRUE.equals(settings.getPushEnabled());
        };
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notification channel is disabled for this event");
        }
    }

    private EventNotificationResponse toResponse(UUID eventId, EventNotificationRequest request, Communication communication) {
        EventNotificationResponse response = new EventNotificationResponse();
        response.setNotificationId(communication.getId());
        response.setEventId(eventId);
        response.setChannel(request.getChannel());
        response.setSubject(request.getSubject());
        response.setContent(request.getContent());
        response.setStatus(communication.getStatus() != null ? communication.getStatus().name().toLowerCase() : null);
        response.setRecipientCount(calculateRecipientCount(request));
        response.setScheduledAt(request.getScheduledAt());
        response.setSentAt(communication.getSentAt());
        response.setPriority(request.getPriority());
        response.setCreatedAt(communication.getCreatedAt());
        response.setSuccessfulRecipients(request.getRecipientEmails() != null ? request.getRecipientEmails() : new ArrayList<>());
        response.setFailedRecipients(new ArrayList<>());
        return response;
    }

    private int calculateRecipientCount(EventNotificationRequest request) {
        int emails = request.getRecipientEmails() != null ? request.getRecipientEmails().size() : 0;
        int users = request.getRecipientUserIds() != null ? request.getRecipientUserIds().size() : 0;
        return emails + users;
    }
}
