package eventplanner.features.event.service;

import eventplanner.common.communication.model.Communication;
import eventplanner.common.communication.repository.CommunicationRepository;
import eventplanner.common.communication.services.core.NotificationService;
import eventplanner.common.communication.services.core.dto.NotificationRequest;
import eventplanner.common.communication.services.core.dto.NotificationResponse;
import eventplanner.common.domain.enums.CommunicationType;
import eventplanner.features.event.dto.request.EventNotificationRequest;
import eventplanner.features.event.dto.response.EventNotificationResponse;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventNotificationSettings;
import eventplanner.features.event.entity.Venue;
import eventplanner.features.event.enums.EmailTemplateType;
import eventplanner.features.event.enums.EventNotificationChannel;
import eventplanner.features.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EventNotificationService {

    private final NotificationService notificationService;
    private final EventNotificationSettingsService settingsService;
    private final CommunicationRepository communicationRepository;
    private final EventRecipientResolverService recipientResolverService;
    private final EventEmailTemplateService emailTemplateService;
    private final EventRepository eventRepository;

    public EventNotificationResponse sendNotification(UUID eventId, EventNotificationRequest request) {
        EventNotificationSettings settings = settingsService.getSettingsEntity(eventId);
        validateChannelEnabled(settings, request.getChannel());

        CommunicationType type = request.getChannel().toCommunicationType();
        
        // Validate email template type for EMAIL channel
        String templateId = resolveTemplateId(request, type);
        log.info("Resolved template ID: {} for template type: {}", templateId, request.getEmailTemplateType());
        
        // Resolve recipients using the recipient resolver service
        EventRecipientResolverService.RecipientInfo recipients = recipientResolverService.resolveRecipients(
            eventId,
            request.getRecipientTypes(),
            request.getRecipientUserIds(),
            request.getRecipientEmails()
        );

        log.info("Resolved {} recipients for event {}: {} emails, {} user IDs", 
                recipients.getTotalCount(), eventId, recipients.getEmails().size(), recipients.getUserIds().size());

        List<String> successfulRecipients = new ArrayList<>();
        List<String> failedRecipients = new ArrayList<>();
        int totalSent = 0;

        // Fetch event details for template variables
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        // Prepare template variables with event details
        Map<String, Object> templateVariables = prepareTemplateVariables(request, event);
        log.debug("Prepared {} template variables for event {}", templateVariables.size(), eventId);

        // Send to email recipients
        if (type == CommunicationType.EMAIL && !recipients.getEmails().isEmpty()) {
            for (String email : recipients.getEmails()) {
                try {
                    log.info("Sending email notification to: {} using template: {}", email, templateId);
                    NotificationRequest notificationRequest = NotificationRequest.builder()
                            .type(type)
                            .to(email)
                            .subject(request.getSubject())
                            .templateId(templateId)
                            .templateVariables(templateVariables)
                            .eventId(eventId)
                            .build();
                    
                    NotificationResponse response = notificationService.send(notificationRequest);
                    
                    if (response.isSuccess()) {
                        successfulRecipients.add(email);
                        totalSent++;
                        log.info("Successfully sent email notification to: {} - Message ID: {}", email, response.getMessageId());
                    } else {
                        failedRecipients.add(email);
                        log.error("Failed to send email notification to: {} - Error: {}", email, response.getErrorMessage());
                    }
                } catch (Exception e) {
                    log.error("Exception while sending notification to email: {} - Error: {}", email, e.getMessage(), e);
                    failedRecipients.add(email);
                }
            }
        }

        // Send to user IDs (for push notifications or when user IDs are available)
        if ((type == CommunicationType.PUSH_NOTIFICATION || type == CommunicationType.EMAIL) 
                && !recipients.getUserIds().isEmpty()) {
            for (UUID userId : recipients.getUserIds()) {
                try {
                    Map<String, Object> data = new HashMap<>(templateVariables);
                    if (type == CommunicationType.PUSH_NOTIFICATION) {
                        data.put("body", request.getContent());
                    }
                    
                    NotificationRequest notificationRequest = NotificationRequest.builder()
                            .type(type)
                            .to(userId.toString())
                            .subject(request.getSubject())
                            .templateId(type == CommunicationType.PUSH_NOTIFICATION ? null : templateId)
                            .templateVariables(data)
                            .eventId(eventId)
                            .build();
                    
                    NotificationResponse response = notificationService.send(notificationRequest);
                    
                    if (response.isSuccess()) {
                        successfulRecipients.add(userId.toString());
                        totalSent++;
                        log.info("Successfully sent notification to user: {} - Message ID: {}", userId, response.getMessageId());
                    } else {
                        failedRecipients.add(userId.toString());
                        log.error("Failed to send notification to user: {} - Error: {}", userId, response.getErrorMessage());
                    }
                } catch (Exception e) {
                    log.error("Exception while sending notification to user: {} - Error: {}", userId, e.getMessage(), e);
                    failedRecipients.add(userId.toString());
                }
            }
        }

        log.info("Notification sending complete for event {}: {} successful, {} failed", 
                eventId, successfulRecipients.size(), failedRecipients.size());
        
        // If all notifications failed, throw an exception
        if (successfulRecipients.isEmpty() && !failedRecipients.isEmpty()) {
            String errorMsg = String.format("All notifications failed to send. Errors: %s", 
                    String.join(", ", failedRecipients));
            log.error(errorMsg);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg);
        }
        
        // If some failed, log warning but don't throw (partial success)
        if (!failedRecipients.isEmpty()) {
            log.warn("Some notifications failed: {}", failedRecipients);
        }

        // Get a sample communication for response (latest one)
        Communication communication = communicationRepository
                .findByEventIdOrderByCreatedAtDesc(eventId)
                .stream()
                .filter(c -> c.getCommunicationType() == type)
                .findFirst()
                .orElse(null);

        return toResponse(eventId, request, communication, recipients, successfulRecipients, failedRecipients);
    }

    /**
     * Resolve the Resend template ID from the request
     * Priority: custom templateId > emailTemplateType mapping
     * 
     * @param request The notification request
     * @param type The communication type
     * @return Resend template ID string, or null if not applicable
     */
    private String resolveTemplateId(EventNotificationRequest request, CommunicationType type) {
        // If custom templateId is provided, use it (takes priority)
        if (request.getTemplateId() != null && !request.getTemplateId().isBlank()) {
            return request.getTemplateId();
        }
        
        // For EMAIL channel, require emailTemplateType
        if (type == CommunicationType.EMAIL) {
            if (request.getEmailTemplateType() == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, 
                    "emailTemplateType is required for EMAIL channel. Options: ANNOUNCEMENT, CANCEL_EVENT"
                );
            }
            return emailTemplateService.getTemplateId(request.getEmailTemplateType());
        }
        
        // For other channels, no template needed
        return null;
    }

    /**
     * Prepare template variables for the email template
     * Includes relevant event details based on template type
     * 
     * @param request The notification request
     * @param event The event entity
     * @return Map of template variables
     */
    private Map<String, Object> prepareTemplateVariables(EventNotificationRequest request, Event event) {
        Map<String, Object> templateVariables = new HashMap<>();
        
        // Basic notification content
        templateVariables.put("content", request.getContent());
        templateVariables.put("subject", request.getSubject());
        templateVariables.put("eventId", event.getId().toString());
        
        // Template type
        EmailTemplateType templateType = request.getEmailTemplateType();
        if (templateType != null) {
            templateVariables.put("templateType", templateType.name());
        }
        
        // Common event information (always included)
        templateVariables.put("eventName", event.getName());
        templateVariables.put("eventDescription", event.getDescription());
        templateVariables.put("eventType", event.getEventType() != null ? event.getEventType().name() : null);
        templateVariables.put("eventStatus", event.getEventStatus() != null ? event.getEventStatus().name() : null);
        
        // Format dates for display
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a");
        
        if (event.getStartDateTime() != null) {
            templateVariables.put("startDate", event.getStartDateTime().format(dateFormatter));
            templateVariables.put("startTime", event.getStartDateTime().format(timeFormatter));
            templateVariables.put("startDateTime", event.getStartDateTime().format(dateTimeFormatter));
            templateVariables.put("startDateTimeISO", event.getStartDateTime().toString());
        }
        
        if (event.getEndDateTime() != null) {
            templateVariables.put("endDate", event.getEndDateTime().format(dateFormatter));
            templateVariables.put("endTime", event.getEndDateTime().format(timeFormatter));
            templateVariables.put("endDateTime", event.getEndDateTime().format(dateTimeFormatter));
            templateVariables.put("endDateTimeISO", event.getEndDateTime().toString());
        }
        
        if (event.getRegistrationDeadline() != null) {
            templateVariables.put("registrationDeadline", event.getRegistrationDeadline().format(dateTimeFormatter));
            templateVariables.put("registrationDeadlineISO", event.getRegistrationDeadline().toString());
        }
        
        // Venue information
        if (event.getVenue() != null) {
            Venue venue = event.getVenue();
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
        
        // Additional event details
        templateVariables.put("coverImageUrl", event.getCoverImageUrl());
        templateVariables.put("eventWebsiteUrl", event.getEventWebsiteUrl());
        templateVariables.put("hashtag", event.getHashtag());
        templateVariables.put("capacity", event.getCapacity());
        templateVariables.put("currentAttendeeCount", event.getCurrentAttendeeCount());
        
        // Template-specific variables
        if (templateType == EmailTemplateType.ANNOUNCEMENT) {
            // For announcements, include all relevant event details
            templateVariables.put("announcementMessage", request.getContent());
            templateVariables.put("includeEventDetails", 
                request.getIncludeEventDetails() != null ? request.getIncludeEventDetails() : true);
        } else if (templateType == EmailTemplateType.CANCEL_EVENT) {
            // For cancellations, include cancellation-specific information
            templateVariables.put("cancellationMessage", request.getContent());
            templateVariables.put("cancellationReason", request.getContent()); // Use content as cancellation reason
            templateVariables.put("originalStartDate", event.getStartDateTime() != null 
                ? event.getStartDateTime().format(dateTimeFormatter) : null);
            templateVariables.put("originalEndDate", event.getEndDateTime() != null 
                ? event.getEndDateTime().format(dateTimeFormatter) : null);
        }
        
        return templateVariables;
    }

    private void validateChannelEnabled(EventNotificationSettings settings, EventNotificationChannel channel) {
        if (channel == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notification channel is required");
        }
        boolean enabled = switch (channel) {
            case EMAIL -> Boolean.TRUE.equals(settings.getEmailEnabled());
            case SMS -> Boolean.TRUE.equals(settings.getSmsEnabled());
            case PUSH -> Boolean.TRUE.equals(settings.getPushEnabled());
        };
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notification channel is disabled for this event");
        }
    }

    private EventNotificationResponse toResponse(UUID eventId, 
                                                 EventNotificationRequest request, 
                                                 Communication communication,
                                                 EventRecipientResolverService.RecipientInfo recipients,
                                                 List<String> successfulRecipients,
                                                 List<String> failedRecipients) {
        EventNotificationResponse response = new EventNotificationResponse();
        if (communication != null) {
            response.setNotificationId(communication.getId());
            response.setStatus(communication.getStatus() != null ? communication.getStatus().name().toLowerCase() : null);
            response.setSentAt(communication.getSentAt());
            response.setCreatedAt(communication.getCreatedAt());
        } else {
            // No communication record found - notification may have failed or not been persisted
            response.setNotificationId(null);
            response.setStatus(successfulRecipients.isEmpty() ? "failed" : "partial");
            response.setSentAt(null);
            response.setCreatedAt(null);
        }
        response.setEventId(eventId);
        response.setChannel(request.getChannel());
        response.setSubject(request.getSubject());
        response.setContent(request.getContent());
        response.setRecipientCount(recipients.getTotalCount());
        response.setScheduledAt(request.getScheduledAt());
        response.setPriority(request.getPriority());
        response.setSuccessfulRecipients(successfulRecipients);
        response.setFailedRecipients(failedRecipients);
        return response;
    }
}
