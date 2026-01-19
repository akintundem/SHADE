package eventplanner.features.event.service;

import eventplanner.common.communication.services.core.BulkNotificationService;
import eventplanner.common.communication.services.core.NotificationTargetResolver;
import eventplanner.common.storage.s3.services.S3StorageService;
import eventplanner.features.budget.service.BudgetService;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.enums.EventStatus;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.event.repository.EventRoleRepository;
import eventplanner.features.event.repository.EventStoredObjectRepository;
import eventplanner.features.feeds.repository.FeedPostRepository;
import eventplanner.features.ticket.repository.TicketTypeRepository;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.authorization.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Black-box tests for the core event service.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRoleRepository eventRoleRepository;

    @Mock
    private FeedPostRepository eventPostRepository;

    @Mock
    private EventStoredObjectRepository eventStoredObjectRepository;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private BudgetService budgetService;

    @Mock
    private UserEventContextService userEventContextService;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private NotificationTargetResolver notificationTargetResolver;

    @Mock
    private BulkNotificationService bulkNotificationService;

    private EventService service;

    @BeforeEach
    void setUp() {
        service = new EventService();

        // Inject all dependencies by type to keep tests focused on public behavior.
        ReflectionTestUtils.setField(service, "eventRepository", eventRepository);
        ReflectionTestUtils.setField(service, "eventRoleRepository", eventRoleRepository);
        ReflectionTestUtils.setField(service, "eventPostRepository", eventPostRepository);
        ReflectionTestUtils.setField(service, "eventStoredObjectRepository", eventStoredObjectRepository);
        ReflectionTestUtils.setField(service, "s3StorageService", s3StorageService);
        ReflectionTestUtils.setField(service, "userAccountRepository", userAccountRepository);
        ReflectionTestUtils.setField(service, "authorizationService", authorizationService);
        ReflectionTestUtils.setField(service, "budgetService", budgetService);
        ReflectionTestUtils.setField(service, "userEventContextService", userEventContextService);
        ReflectionTestUtils.setField(service, "ticketTypeRepository", ticketTypeRepository);
        ReflectionTestUtils.setField(service, "notificationTargetResolver", notificationTargetResolver);
        ReflectionTestUtils.setField(service, "bulkNotificationService", bulkNotificationService);
    }

    @Test
    void getById_returnsOptional_whenEventExists() {
        UUID eventId = UUID.randomUUID();
        Event event = TestFixtures.minimalEvent(eventId);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        Optional<Event> result = service.getById(eventId);

        assertTrue(result.isPresent());
        assertSame(event, result.get());
    }

    @Test
    void getById_returnsEmpty_whenEventMissing() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        Optional<Event> result = service.getById(eventId);

        assertFalse(result.isPresent());
    }

    @Test
    void updateEventStatus_throws_whenEventIdIsNull() {
        // Edge: null identifiers should be rejected.
        assertThrows(RuntimeException.class, () -> service.updateEventStatus(null, EventStatus.DRAFT));
    }
}
