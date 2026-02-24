package eventplanner.features.event.service;

import eventplanner.features.event.enums.EmailTemplateType;
import eventplanner.common.exception.exceptions.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to map EmailTemplateType enum to Resend template IDs
 * Templates are configured in Resend dashboard, and we map our enum values to those template IDs
 */
@Service
@Slf4j
public class EventEmailTemplateService {

    /**
     * Map of EmailTemplateType to Resend template IDs
     * These template IDs must match the templates configured in your Resend dashboard
     */
    private static final Map<EmailTemplateType, String> TEMPLATE_ID_MAP = new HashMap<>();

    static {
        // Template IDs must match the `id` field in email/templates/index.ts exactly.
        // When a new template is added to the email service, add a matching entry here.
        TEMPLATE_ID_MAP.put(EmailTemplateType.ANNOUNCEMENT,             "general-announcement");
        TEMPLATE_ID_MAP.put(EmailTemplateType.CANCEL_EVENT,             "event-cancellation-notice");
        TEMPLATE_ID_MAP.put(EmailTemplateType.EVENT_REMINDER,           "event-reminder");
        TEMPLATE_ID_MAP.put(EmailTemplateType.ATTENDEE_WELCOME,         "attendee-welcome");
        TEMPLATE_ID_MAP.put(EmailTemplateType.ATTENDEE_INVITE,          "attendee-invite");
        TEMPLATE_ID_MAP.put(EmailTemplateType.ATTENDEE_INVITE_RESPONSE, "attendee-invite-response");
        TEMPLATE_ID_MAP.put(EmailTemplateType.TICKET_CONFIRMATION,      "ticket-confirmation");
        TEMPLATE_ID_MAP.put(EmailTemplateType.COLLABORATOR_INVITE,      "event-invitation");
        TEMPLATE_ID_MAP.put(EmailTemplateType.COLLABORATOR_WELCOME,     "collaborator-welcome");
    }

    /**
     * Get Resend template ID for the given email template type
     * 
     * @param templateType The email template type enum
     * @return Resend template ID string, or null if template type is null
     */
    public String getTemplateId(EmailTemplateType templateType) {
        if (templateType == null) {
            return null;
        }
        
        String templateId = TEMPLATE_ID_MAP.get(templateType);
        if (templateId == null) {
            log.error("No template ID mapping found for template type: {}", templateType);
            throw new BadRequestException("Invalid email template type: " + templateType);
        }
        
        return templateId;
    }

    /**
     * Check if a template type has a mapping
     * 
     * @param templateType The email template type enum
     * @return true if mapping exists, false otherwise
     */
    public boolean hasTemplateMapping(EmailTemplateType templateType) {
        return templateType != null && TEMPLATE_ID_MAP.containsKey(templateType);
    }
}
