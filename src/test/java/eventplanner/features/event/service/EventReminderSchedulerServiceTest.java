package eventplanner.features.event.service;

import eventplanner.common.communication.enums.CommunicationStatus;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationResponse;
import eventplanner.common.config.ExternalServicesProperties;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventReminder;
import eventplanner.features.event.enums.EmailTemplateType;
import eventplanner.features.event.repository.EventReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Scheduler tests focus on behavior for empty and minimal reminder batches.
 */
@ExtendWith(MockitoExtension.class)
class EventReminderSchedulerServiceTest {
    @Mock
    private EventReminderRepository reminderRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EventEmailTemplateService emailTemplateService;

    @Mock
    private EventTemplateVariableService templateVariableService;

    @Mock
    private ExternalServicesProperties externalServicesProperties;

    private EventReminderSchedulerService service;

    @BeforeEach
    void setUp() {
        service = new EventReminderSchedulerService(
                reminderRepository,
                notificationService,
                emailTemplateService,
                templateVariableService,
                externalServicesProperties
        );
    }

    @Test
    void sendPendingReminders_doesNothing_whenNoRemindersExist() {
        when(reminderRepository.findPendingRemindersToSend(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(service::sendPendingReminders);
        verify(notificationService, never()).send(any());
    }

    @Test
    void sendPendingReminders_doesNotThrow_withSingleMinimalReminder() {
        Event event = TestFixtures.minimalEvent(UUID.randomUUID());
        EventReminder reminder = new EventReminder();
        reminder.setEvent(event);
        reminder.setTitle("Reminder");
        reminder.setReminderTime(LocalDateTime.now().minusMinutes(1));
        reminder.setChannel("EMAIL");
        reminder.setEmailTemplateType(EmailTemplateType.ANNOUNCEMENT.name());
        reminder.setRecipientEmailsCsv("recipient@example.com");
        reminder.setRecipientUserIdsCsv("");
        reminder.setIsActive(true);

        ExternalServicesProperties.Email email = new ExternalServicesProperties.Email();
        email.setFrom("no-reply@example.com");
        email.setFromEvents("events@example.com");

        when(reminderRepository.findPendingRemindersToSend(any(LocalDateTime.class)))
                .thenReturn(List.of(reminder));
        when(emailTemplateService.getTemplateId(any())).thenReturn("template-1");
        when(templateVariableService.prepareTemplateVariables(any(), anyString(), anyString(), any()))
                .thenReturn(Collections.emptyMap());
        lenient().when(externalServicesProperties.getEmail()).thenReturn(email);
        lenient().when(notificationService.send(any()))
                .thenReturn(NotificationResponse.success(UUID.randomUUID(), "msg-id", CommunicationStatus.SENT));

        assertDoesNotThrow(service::sendPendingReminders);
    }
}
