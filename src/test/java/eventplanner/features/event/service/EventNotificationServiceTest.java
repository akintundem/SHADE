package eventplanner.features.event.service;

import eventplanner.common.communication.enums.CommunicationStatus;
import eventplanner.common.communication.repository.CommunicationRepository;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationResponse;
import eventplanner.common.config.ExternalServicesProperties;
import eventplanner.features.event.dto.request.EventNotificationRequest;
import eventplanner.features.event.dto.response.EventNotificationResponse;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventNotificationSettings;
import eventplanner.features.event.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Black-box tests validating event notification flows with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class EventNotificationServiceTest {
    @Mock
    private NotificationService notificationService;

    @Mock
    private EventNotificationSettingsService settingsService;

    @Mock
    private CommunicationRepository communicationRepository;

    @Mock
    private EventRecipientResolverService recipientResolverService;

    @Mock
    private EventEmailTemplateService emailTemplateService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventTemplateVariableService templateVariableService;

    @Mock
    private ExternalServicesProperties externalServicesProperties;

    private EventNotificationService service;

    @BeforeEach
    void setUp() {
        service = new EventNotificationService(
                notificationService,
                settingsService,
                communicationRepository,
                recipientResolverService,
                emailTemplateService,
                eventRepository,
                templateVariableService,
                externalServicesProperties
        );
    }

    @Test
    void sendNotification_returnsResponse_whenDependenciesAllow() {
        UUID eventId = UUID.randomUUID();
        Event event = TestFixtures.minimalEvent(eventId);
        EventNotificationSettings settings = TestFixtures.notificationSettingsFor(event);
        EventNotificationRequest request = TestFixtures.basicNotificationRequest("recipient@example.com");
        EventRecipientResolverService.RecipientInfo recipients =
                new EventRecipientResolverService.RecipientInfo(Set.of(UUID.randomUUID()), Set.of("recipient@example.com"));

        ExternalServicesProperties.Email email = new ExternalServicesProperties.Email();
        email.setFrom("no-reply@example.com");
        email.setFromEvents("events@example.com");

        // Stubs for the minimal happy path.
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(settingsService.getSettingsEntity(eventId)).thenReturn(settings);
        when(recipientResolverService.resolveRecipients(eq(eventId), any(), any(), any())).thenReturn(recipients);
        when(emailTemplateService.getTemplateId(any())).thenReturn("template-1");
        when(templateVariableService.prepareTemplateVariables(eq(event), anyString(), anyString(), any()))
                .thenReturn(java.util.Collections.emptyMap());
        when(notificationService.send(any())).thenReturn(NotificationResponse.success(UUID.randomUUID(), "msg-id", CommunicationStatus.SENT));
        lenient().when(externalServicesProperties.getEmail()).thenReturn(email);

        EventNotificationResponse response = service.sendNotification(eventId, request);

        assertNotNull(response);
    }

    @Test
    void sendNotification_throws_whenEventMissing() {
        UUID eventId = UUID.randomUUID();
        EventNotificationRequest request = TestFixtures.basicNotificationRequest("recipient@example.com");

        // Edge: missing events should be rejected.
        assertThrows(RuntimeException.class, () -> service.sendNotification(eventId, request));
    }

    @Test
    void sendNotification_throws_whenRequestIsNull() {
        UUID eventId = UUID.randomUUID();

        // Edge: null requests should not be accepted.
        assertThrows(RuntimeException.class, () -> service.sendNotification(eventId, null));
    }
}
