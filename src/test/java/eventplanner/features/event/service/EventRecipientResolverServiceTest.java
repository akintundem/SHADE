package eventplanner.features.event.service;

import eventplanner.features.attendee.repository.AttendeeRepository;
import eventplanner.features.collaboration.repository.EventUserRepository;
import eventplanner.security.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Black-box tests for recipient resolution.
 */
@ExtendWith(MockitoExtension.class)
class EventRecipientResolverServiceTest {
    @Mock
    private EventUserRepository eventUserRepository;

    @Mock
    private AttendeeRepository attendeeRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    private EventRecipientResolverService service;

    @BeforeEach
    void setUp() {
        service = new EventRecipientResolverService(eventUserRepository, attendeeRepository, userAccountRepository);
    }

    @Test
    void resolveRecipients_returnsEmpty_whenInputsAreEmpty() {
        UUID eventId = UUID.randomUUID();

        EventRecipientResolverService.RecipientInfo info = service.resolveRecipients(
                eventId,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertNotNull(info);
        assertEquals(0, info.getTotalCount());
    }

    @Test
    void resolveRecipients_allowsNullRecipientTypes() {
        UUID eventId = UUID.randomUUID();

        // Edge: null recipient types should not crash resolution.
        EventRecipientResolverService.RecipientInfo info = assertDoesNotThrow(
                () -> service.resolveRecipients(eventId, null, List.of(), List.of())
        );
        assertNotNull(info);
    }
}
