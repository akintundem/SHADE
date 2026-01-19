package eventplanner.features.event.service;

import eventplanner.features.event.dto.response.EventReminderResponse;
import eventplanner.features.event.repository.EventReminderRepository;
import eventplanner.features.event.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Black-box tests for reminder CRUD behaviors.
 */
@ExtendWith(MockitoExtension.class)
class EventReminderServiceTest {
    @Mock
    private EventReminderRepository reminderRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRecipientResolverService recipientResolverService;

    private EventReminderService service;

    @BeforeEach
    void setUp() {
        service = new EventReminderService(reminderRepository, eventRepository, recipientResolverService);
    }

    @Test
    void list_returnsEmpty_whenRepositoryHasNoReminders() {
        UUID eventId = UUID.randomUUID();
        when(reminderRepository.findByEventId(eq(eventId), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        java.util.List<EventReminderResponse> results = service.list(eventId, 0, 10);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void get_throws_whenReminderIsMissing() {
        UUID eventId = UUID.randomUUID();
        UUID reminderId = UUID.randomUUID();

        when(reminderRepository.findById(reminderId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.get(eventId, reminderId));
    }
}
