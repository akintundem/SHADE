package eventplanner.features.event.service;

import eventplanner.features.config.AppProperties;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.enums.EmailTemplateType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Black-box tests for email template variable preparation.
 */
class EventTemplateVariableServiceTest {
    private final EventTemplateVariableService service = new EventTemplateVariableService(new AppProperties());

    @Test
    void prepareTemplateVariables_returnsNonEmptyMap_forMinimalEvent() {
        Event event = TestFixtures.minimalEvent(UUID.randomUUID());

        Map<String, Object> variables = service.prepareTemplateVariables(
                event,
                "Organizer Name",
                "organizer@example.com",
                EmailTemplateType.ANNOUNCEMENT
        );

        assertNotNull(variables);
        assertFalse(variables.isEmpty());
    }

    @Test
    void prepareTemplateVariables_throws_whenEventIsNull() {
        // Edge: null event should not be accepted.
        assertThrows(RuntimeException.class, () -> service.prepareTemplateVariables(
                null,
                "Organizer Name",
                "organizer@example.com",
                EmailTemplateType.ANNOUNCEMENT
        ));
    }
}
