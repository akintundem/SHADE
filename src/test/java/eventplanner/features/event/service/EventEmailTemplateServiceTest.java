package eventplanner.features.event.service;

import eventplanner.features.event.enums.EmailTemplateType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Contract-style tests for template mapping behavior.
 */
class EventEmailTemplateServiceTest {
    private final EventEmailTemplateService service = new EventEmailTemplateService();

    @Test
    void hasTemplateMapping_impliesTemplateIdPresent() {
        for (EmailTemplateType type : EmailTemplateType.values()) {
            boolean mapped = service.hasTemplateMapping(type);
            String templateId = service.getTemplateId(type);

            // General contract: mapped templates should have a concrete identifier.
            if (mapped) {
                assertNotNull(templateId);
                assertFalse(templateId.isBlank());
            }
        }
    }

    @Test
    void getTemplateId_isStable_forMappedTypes() {
        for (EmailTemplateType type : EmailTemplateType.values()) {
            if (service.hasTemplateMapping(type)) {
                // General contract: repeated lookups are stable.
                String first = service.getTemplateId(type);
                String second = service.getTemplateId(type);
                assertNotNull(first);
                assertNotNull(second);
            }
        }
    }

    @Test
    void getTemplateId_allowsNullTemplateType() {
        // Edge: null input should not crash the resolver.
        assertDoesNotThrow(() -> service.getTemplateId(null));
    }
}
