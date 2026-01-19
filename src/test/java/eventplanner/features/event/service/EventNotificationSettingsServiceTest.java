package eventplanner.features.event.service;

import eventplanner.features.event.dto.request.EventNotificationSettingsRequest;
import eventplanner.features.event.dto.response.EventNotificationSettingsResponse;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventNotificationSettings;
import eventplanner.features.event.repository.EventNotificationSettingsRepository;
import eventplanner.features.event.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Black-box tests for notification settings management.
 */
@ExtendWith(MockitoExtension.class)
class EventNotificationSettingsServiceTest {
    @Mock
    private EventNotificationSettingsRepository settingsRepository;

    @Mock
    private EventRepository eventRepository;

    private EventNotificationSettingsService service;

    @BeforeEach
    void setUp() {
        service = new EventNotificationSettingsService(settingsRepository, eventRepository);
    }

    @Test
    void getSettings_returnsResponse_whenSettingsExist() {
        UUID eventId = UUID.randomUUID();
        Event event = TestFixtures.minimalEvent(eventId);
        EventNotificationSettings settings = TestFixtures.notificationSettingsFor(event);

        when(settingsRepository.findByEventId(eventId)).thenReturn(Optional.of(settings));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        EventNotificationSettingsResponse response = service.getSettings(eventId);

        assertNotNull(response);
    }

    @Test
    void getSettingsEntity_throws_whenMissing() {
        UUID eventId = UUID.randomUUID();

        // Edge: missing settings should be rejected.
        assertThrows(RuntimeException.class, () -> service.getSettingsEntity(eventId));
    }

    @Test
    void updateSettings_throws_whenRequestIsNull() {
        UUID eventId = UUID.randomUUID();

        // Edge: null request should not be accepted.
        assertThrows(RuntimeException.class, () -> service.updateSettings(eventId, (EventNotificationSettingsRequest) null));
    }
}
