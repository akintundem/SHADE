package eventplanner.features.event.service;

import eventplanner.features.event.dto.request.EventNotificationRequest;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventNotificationSettings;
import eventplanner.features.event.enums.EventAccessType;
import eventplanner.features.event.enums.EventNotificationChannel;
import eventplanner.features.event.enums.EventNotificationPriority;
import eventplanner.features.event.enums.EventStatus;
import eventplanner.features.event.enums.EventType;
import eventplanner.features.event.enums.EmailTemplateType;
import eventplanner.features.event.enums.RecipientType;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.service.UserPrincipal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

final class TestFixtures {
    private TestFixtures() {
    }

    static Event minimalEvent(UUID eventId) {
        Event event = new Event();
        event.setId(eventId);
        event.setName("Test Event");
        event.setEventType(EventType.CONFERENCE);
        event.setEventStatus(EventStatus.DRAFT);
        event.setStartDateTime(LocalDateTime.now().plusDays(1));
        event.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(2));
        event.setAccessType(EventAccessType.OPEN);
        event.setIsPublic(true);
        event.setCapacity(100);
        event.setCurrentAttendeeCount(0);
        return event;
    }

    static UserPrincipal userPrincipal(UUID userId) {
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setName("Test User");
        user.setEmail("test.user@example.com");
        return new UserPrincipal(user);
    }

    static EventNotificationRequest basicNotificationRequest(String email) {
        EventNotificationRequest request = new EventNotificationRequest();
        request.setChannel(EventNotificationChannel.EMAIL);
        request.setSubject("Test Subject");
        request.setContent("Test Content");
        request.setRecipientTypes(List.of(RecipientType.SPECIFIC_PERSON));
        request.setRecipientEmails(List.of(email));
        request.setRecipientUserIds(List.of());
        request.setIncludeEventDetails(Boolean.FALSE);
        request.setPriority(EventNotificationPriority.NORMAL);
        request.setEmailTemplateType(EmailTemplateType.ANNOUNCEMENT);
        return request;
    }

    static EventNotificationSettings notificationSettingsFor(Event event) {
        EventNotificationSettings settings = new EventNotificationSettings();
        settings.setEvent(event);
        settings.setEmailEnabled(true);
        settings.setSmsEnabled(false);
        settings.setPushEnabled(false);
        settings.setReminderEnabled(true);
        settings.setDefaultReminderMinutes(60);
        return settings;
    }
}
