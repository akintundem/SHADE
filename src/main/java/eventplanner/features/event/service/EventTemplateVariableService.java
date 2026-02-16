package eventplanner.features.event.service;

import eventplanner.features.config.AppProperties;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.Venue;
import eventplanner.features.event.enums.EmailTemplateType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared utility service for preparing email template variables
 * Used by both EventNotificationService and EventReminderSchedulerService
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventTemplateVariableService {

    private final AppProperties appProperties;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a");

    /**
     * Prepare template variables for email templates with full event details
     * 
     * @param event The event entity
     * @param content The message content
     * @param subject The email subject
     * @param templateType The email template type (optional)
     * @return Map of template variables ready for Resend templates
     */
    public Map<String, Object> prepareTemplateVariables(Event event, String content, String subject, EmailTemplateType templateType) {
        Map<String, Object> templateVariables = new HashMap<>();
        
        // Basic content
        templateVariables.put("content", content);
        templateVariables.put("subject", subject);
        templateVariables.put("eventId", event.getId().toString());
        
        // Template type
        if (templateType != null) {
            templateVariables.put("templateType", templateType.name());
        }
        
        // Common event information (always included)
        templateVariables.put("eventName", event.getName());
        templateVariables.put("eventDescription", event.getDescription());
        templateVariables.put("eventType", event.getEventType() != null ? event.getEventType().name() : null);
        templateVariables.put("eventStatus", event.getEventStatus() != null ? event.getEventStatus().name() : null);
        
        // Format dates for display
        addFormattedDates(templateVariables, event);
        
        // Venue information
        addVenueInformation(templateVariables, event.getVenue());
        
        // Additional event details; actionUrl is sanitized in addTemplateSpecificVariables
        templateVariables.put("coverImageUrl", event.getCoverImageUrl());
        templateVariables.put("eventWebsiteUrl", event.getEventWebsiteUrl());
        templateVariables.put("hashtag", event.getHashtag());
        templateVariables.put("capacity", event.getCapacity());
        templateVariables.put("currentAttendeeCount", event.getCurrentAttendeeCount());
        
        // Template-specific variables
        addTemplateSpecificVariables(templateVariables, templateType, content, event);
        
        return templateVariables;
    }

    /**
     * Add formatted date fields to template variables
     */
    private void addFormattedDates(Map<String, Object> templateVariables, Event event) {
        if (event.getStartDateTime() != null) {
            templateVariables.put("startDate", event.getStartDateTime().format(DATE_FORMATTER));
            templateVariables.put("startTime", event.getStartDateTime().format(TIME_FORMATTER));
            templateVariables.put("startDateTime", event.getStartDateTime().format(DATE_TIME_FORMATTER));
            templateVariables.put("startDateTimeISO", event.getStartDateTime().toString());
        }
        
        if (event.getEndDateTime() != null) {
            templateVariables.put("endDate", event.getEndDateTime().format(DATE_FORMATTER));
            templateVariables.put("endTime", event.getEndDateTime().format(TIME_FORMATTER));
            templateVariables.put("endDateTime", event.getEndDateTime().format(DATE_TIME_FORMATTER));
            templateVariables.put("endDateTimeISO", event.getEndDateTime().toString());
        }
        
        if (event.getRegistrationDeadline() != null) {
            templateVariables.put("registrationDeadline", event.getRegistrationDeadline().format(DATE_TIME_FORMATTER));
            templateVariables.put("registrationDeadlineISO", event.getRegistrationDeadline().toString());
        }
    }

    /**
     * Add venue information to template variables
     */
    private void addVenueInformation(Map<String, Object> templateVariables, Venue venue) {
        if (venue == null) {
            return;
        }
        
        Map<String, Object> venueInfo = new HashMap<>();
        venueInfo.put("address", venue.getAddress());
        venueInfo.put("city", venue.getCity());
        venueInfo.put("state", venue.getState());
        venueInfo.put("country", venue.getCountry());
        venueInfo.put("zipCode", venue.getZipCode());
        
        // Build full address string
        List<String> addressParts = new ArrayList<>();
        if (venue.getAddress() != null) addressParts.add(venue.getAddress());
        if (venue.getCity() != null) addressParts.add(venue.getCity());
        if (venue.getState() != null) addressParts.add(venue.getState());
        if (venue.getZipCode() != null) addressParts.add(venue.getZipCode());
        if (venue.getCountry() != null) addressParts.add(venue.getCountry());
        venueInfo.put("fullAddress", String.join(", ", addressParts));
        
        if (venue.getLatitude() != null && venue.getLongitude() != null) {
            venueInfo.put("latitude", venue.getLatitude());
            venueInfo.put("longitude", venue.getLongitude());
        }
        
        templateVariables.put("venue", venueInfo);
    }

    /**
     * Add template-specific variables based on template type
     */
    private void addTemplateSpecificVariables(Map<String, Object> templateVariables, 
                                              EmailTemplateType templateType, 
                                              String content, 
                                              Event event) {
        if (templateType == null) {
            return;
        }
        
        switch (templateType) {
            case ANNOUNCEMENT:
                templateVariables.put("eventName", event.getName());
                templateVariables.put("subjectLine", templateVariables.getOrDefault("subject", content));
                templateVariables.put("eventDate", templateVariables.getOrDefault("startDateTime", templateVariables.get("startDate")));
                Object venue = templateVariables.get("venue");
                if (venue instanceof Map<?, ?> venueMap && venueMap.get("fullAddress") != null) {
                    templateVariables.put("venue", venueMap.get("fullAddress"));
                }
                templateVariables.put("highlight", content);
                templateVariables.put("actionUrl", sanitizeEventWebsiteUrl(event.getEventWebsiteUrl(), event.getId()));
                break;
            case CANCEL_EVENT:
                templateVariables.put("eventName", event.getName());
                templateVariables.put("reason", content);
                templateVariables.put("actionUrl", sanitizeEventWebsiteUrl(event.getEventWebsiteUrl(), event.getId()));
                break;
            default:
                // No additional variables for other types
                break;
        }
    }

    /**
     * Allowlist event website URL for emails to prevent phishing. Only same-origin or relative paths allowed.
     * Otherwise returns app base URL + event path.
     */
    private String sanitizeEventWebsiteUrl(String eventWebsiteUrl, java.util.UUID eventId) {
        if (!StringUtils.hasText(eventWebsiteUrl)) {
            return appBaseUrlOrEventPath(eventId);
        }
        String base = appProperties.getBaseUrl();
        if (!StringUtils.hasText(base)) {
            return appBaseUrlOrEventPath(eventId);
        }
        String normalized = eventWebsiteUrl.trim().toLowerCase();
        String baseNorm = base.endsWith("/") ? base.toLowerCase() : (base + "/").toLowerCase();
        if (normalized.startsWith(baseNorm) || normalized.equals(base.toLowerCase())) {
            return eventWebsiteUrl.trim();
        }
        if (normalized.startsWith("/")) {
            return (base.endsWith("/") ? base : base + "/") + eventWebsiteUrl.trim().substring(1);
        }
        log.debug("Rejecting external eventWebsiteUrl for email actionUrl, using app base");
        return appBaseUrlOrEventPath(eventId);
    }

    private String appBaseUrlOrEventPath(java.util.UUID eventId) {
        String base = appProperties.getBaseUrl();
        if (!StringUtils.hasText(base)) {
            return "";
        }
        String path = base.endsWith("/") ? base : base + "/";
        return eventId != null ? path + "events/" + eventId : path;
    }
}
