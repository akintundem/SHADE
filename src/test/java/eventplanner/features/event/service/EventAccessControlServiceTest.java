package eventplanner.features.event.service;

import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.security.auth.service.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Black-box tests derived from public signatures only; no implementation details are assumed.
 */
@ExtendWith(MockitoExtension.class)
class EventAccessControlServiceTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventUserRepository eventUserRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AttendeeRepository attendeeRepository;

    private EventAccessControlService service;

    @BeforeEach
    void setUp() {
        service = new EventAccessControlService(
                eventRepository,
                eventUserRepository,
                ticketRepository,
                attendeeRepository
        );
    }

    @Test
    void ensureEventExists_returnsEvent_whenFound() {
        UUID eventId = UUID.randomUUID();
        Event event = TestFixtures.minimalEvent(eventId);

        // Given a repository hit, the service should return the same event.
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        Event result = service.ensureEventExists(eventId);

        assertSame(event, result);
    }

    @Test
    void ensureEventExists_throws_whenMissing() {
        UUID eventId = UUID.randomUUID();

        // Edge: missing event should be rejected.
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.ensureEventExists(eventId));
    }

    @Test
    void requireMediaView_allowsNullPrincipal_whenEventIsAccessible() {
        UUID eventId = UUID.randomUUID();
        Event event = TestFixtures.minimalEvent(eventId);

        // Observed behavior: null principals are tolerated for accessible events.
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        Event result = assertDoesNotThrow(() -> service.requireMediaView(null, eventId));
        assertSame(event, result);
    }
}
