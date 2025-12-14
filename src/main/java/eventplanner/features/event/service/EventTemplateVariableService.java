package eventplanner.features.event.service;

import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.Venue;
import eventplanner.features.event.enums.EmailTemplateType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        
        // Additional event details
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
                templateVariables.put("announcementMessage", content);
                break;
            case CANCEL_EVENT:
                templateVariables.put("cancellationMessage", content);
                templateVariables.put("cancellationReason", content);
                if (event.getStartDateTime() != null) {
                    templateVariables.put("originalStartDate", event.getStartDateTime().format(DATE_TIME_FORMATTER));
                }
                if (event.getEndDateTime() != null) {
                    templateVariables.put("originalEndDate", event.getEndDateTime().format(DATE_TIME_FORMATTER));
                }
                break;
            default:
                // No additional variables for other types
                break;
        }
    }
}
