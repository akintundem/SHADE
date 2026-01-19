package eventplanner.features.event.service;

import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.features.event.dto.response.UserEventContext;
import eventplanner.features.event.entity.Event;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.security.auth.service.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Black-box tests for building user-specific event context.
 */
@ExtendWith(MockitoExtension.class)
class UserEventContextServiceTest {
    @Mock
    private EventUserRepository eventUserRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AttendeeRepository attendeeRepository;

    private UserEventContextService service;

    @BeforeEach
    void setUp() {
        service = new UserEventContextService(eventUserRepository, ticketRepository, attendeeRepository);
    }

    @Test
    void buildContext_returnsNonNull_forOpenEvent() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Event event = TestFixtures.minimalEvent(eventId);
        UserPrincipal principal = TestFixtures.userPrincipal(userId);

        // Lenient stubs keep the test resilient to internal membership lookups.
        lenient().when(eventUserRepository.findByEventIdAndUserId(eq(eventId), eq(userId)))
                .thenReturn(Optional.empty());
        lenient().when(attendeeRepository.findByEventIdAndUserId(eq(eventId), eq(userId)))
                .thenReturn(Optional.empty());
        lenient().when(ticketRepository.hasValidTicketByUserId(eq(eventId), eq(userId))).thenReturn(false);
        lenient().when(ticketRepository.findValidTicketsByUserId(eq(eventId), eq(userId)))
                .thenReturn(Collections.emptyList());

        UserEventContext context = service.buildContext(event, principal);

        assertNotNull(context);
    }

    @Test
    void buildContext_allowsNullPrincipal() {
        Event event = TestFixtures.minimalEvent(UUID.randomUUID());

        // Edge: null principal should not crash context building.
        assertDoesNotThrow(() -> service.buildContext(event, null));
    }
}
