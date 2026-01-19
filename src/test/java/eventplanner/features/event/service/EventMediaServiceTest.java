package eventplanner.features.event.service;

import eventplanner.common.storage.s3.services.S3StorageService;
import eventplanner.features.event.dto.response.EventMediaResponse;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.event.repository.EventStoredObjectRepository;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Black-box tests for media operations using minimal stubbed dependencies.
 */
@ExtendWith(MockitoExtension.class)
class EventMediaServiceTest {
    @Mock
    private EventAccessControlService accessControlService;

    @Mock
    private S3StorageService storageService;

    @Mock
    private EventStoredObjectRepository storedObjectRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    private EventMediaService service;

    @BeforeEach
    void setUp() {
        service = new EventMediaService(
                accessControlService,
                storageService,
                storedObjectRepository,
                eventRepository,
                userAccountRepository
        );
    }

    @Test
    void getEventMedia_returnsEmptyList_whenNoStoredObjects() {
        UUID eventId = UUID.randomUUID();
        UserPrincipal principal = TestFixtures.userPrincipal(UUID.randomUUID());
        Event event = TestFixtures.minimalEvent(eventId);

        // Given access is allowed and no stored objects exist, the result should be empty.
        when(accessControlService.requireMediaView(principal, eventId)).thenReturn(event);
        when(storedObjectRepository.findByEventIdAndPurposeOrderByCreatedAtDesc(eq(eventId), anyString()))
                .thenReturn(Collections.emptyList());

        List<EventMediaResponse> responses = service.getEventMedia(eventId, principal, "", "");

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void createMediaUpload_throws_whenRequestIsNull() {
        UUID eventId = UUID.randomUUID();
        UserPrincipal principal = TestFixtures.userPrincipal(UUID.randomUUID());

        // Edge: null request should not be accepted.
        assertThrows(RuntimeException.class, () -> service.createMediaUpload(eventId, principal, null));
    }

    @Test
    void getMedia_throws_whenMediaIdIsNull() {
        UUID eventId = UUID.randomUUID();
        UserPrincipal principal = TestFixtures.userPrincipal(UUID.randomUUID());

        // Edge: invalid media id should be rejected.
        assertThrows(RuntimeException.class, () -> service.getMedia(eventId, null, principal));
    }
}
