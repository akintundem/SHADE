package eventplanner.features.event.service;

import eventplanner.features.event.dto.request.EventReminderRequest;
import eventplanner.features.event.dto.response.EventReminderResponse;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventReminder;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.event.repository.EventReminderRepository;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class EventReminderService {

    private final EventReminderRepository reminderRepository;
    private final EventRepository eventRepository;
    private final EventRecipientResolverService recipientResolverService;

    public List<EventReminderResponse> list(UUID eventId, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return reminderRepository.findByEventId(eventId, pageable)
                .stream().map(EventReminderResponse::from).collect(Collectors.toList());
    }

    public EventReminderResponse create(UUID eventId, EventReminderRequest request) {
        // Validate email template type for email channel
        if ("email".equalsIgnoreCase(request.getChannel()) && request.getEmailTemplateType() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "emailTemplateType is required for EMAIL channel reminders. Options: ANNOUNCEMENT, CANCEL_EVENT"
            );
        }
        
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        
        EventReminder reminder = new EventReminder();
        reminder.setEvent(event);
        reminder.setTitle(request.getTitle());
        reminder.setDescription(request.getDescription());
        
        // Set reminder time: use provided time or default to 5 minutes from now
        if (request.getReminderTime() != null) {
            reminder.setReminderTime(request.getReminderTime());
        } else {
            reminder.setReminderTime(LocalDateTime.now().plusMinutes(5));
            log.info("Reminder time not provided, setting to 5 minutes from now: {}", reminder.getReminderTime());
        }
        
        reminder.setChannel(request.getChannel());
        reminder.setReminderType(request.getReminderType());
        reminder.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        reminder.setCustomMessage(request.getCustomMessage());
        
        // Store email template type
        if (request.getEmailTemplateType() != null) {
            reminder.setEmailTemplateType(request.getEmailTemplateType().name());
        }
        
        reminder.setWasSent(false);
        
        // Resolve recipients if recipient types are specified
        if (request.getRecipientTypes() != null && !request.getRecipientTypes().isEmpty()) {
            EventRecipientResolverService.RecipientInfo recipients = recipientResolverService.resolveRecipients(
                eventId,
                request.getRecipientTypes(),
                request.getRecipientUserIds(),
                request.getRecipientEmails()
            );
            reminder.setRecipientUserIdsCsv(joinCsv(recipients.getUserIds().stream().toList()));
            reminder.setRecipientEmailsCsv(joinCsv(recipients.getEmails().stream().toList()));
            log.info("Created reminder for event {} with {} recipients, scheduled for {}", 
                    eventId, recipients.getTotalCount(), reminder.getReminderTime());
        } else {
            // Backward compatibility: use specific recipients if no types specified
            reminder.setRecipientUserIdsCsv(joinCsv(request.getRecipientUserIds()));
            reminder.setRecipientEmailsCsv(joinCsv(request.getRecipientEmails()));
        }
        
        EventReminder saved = reminderRepository.save(reminder);
        return EventReminderResponse.from(saved);
    }

    public EventReminderResponse update(UUID eventId, UUID reminderId, EventReminderRequest request) {
        EventReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found"));
        if (reminder.getEvent() == null || !reminder.getEvent().getId().equals(eventId)) {
            throw new BadRequestException("Reminder does not belong to event");
        }
        if (request.getTitle() != null) reminder.setTitle(request.getTitle());
        if (request.getDescription() != null) reminder.setDescription(request.getDescription());
        if (request.getReminderTime() != null) reminder.setReminderTime(request.getReminderTime());
        if (request.getChannel() != null) reminder.setChannel(request.getChannel());
        if (request.getReminderType() != null) reminder.setReminderType(request.getReminderType());
        if (request.getIsActive() != null) reminder.setIsActive(request.getIsActive());
        if (request.getCustomMessage() != null) reminder.setCustomMessage(request.getCustomMessage());
        
        // Update email template type
        if (request.getEmailTemplateType() != null) {
            reminder.setEmailTemplateType(request.getEmailTemplateType().name());
        }
        
        // Update recipients if recipient types are specified
        if (request.getRecipientTypes() != null && !request.getRecipientTypes().isEmpty()) {
            EventRecipientResolverService.RecipientInfo recipients = recipientResolverService.resolveRecipients(
                eventId,
                request.getRecipientTypes(),
                request.getRecipientUserIds(),
                request.getRecipientEmails()
            );
            reminder.setRecipientUserIdsCsv(joinCsv(recipients.getUserIds().stream().toList()));
            reminder.setRecipientEmailsCsv(joinCsv(recipients.getEmails().stream().toList()));
        } else {
            // Backward compatibility: update specific recipients if provided
            if (request.getRecipientUserIds() != null) {
                reminder.setRecipientUserIdsCsv(joinCsv(request.getRecipientUserIds()));
            }
            if (request.getRecipientEmails() != null) {
                reminder.setRecipientEmailsCsv(joinCsv(request.getRecipientEmails()));
            }
        }
        
        return EventReminderResponse.from(reminder);
    }

    public void delete(UUID eventId, UUID reminderId) {
        EventReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found"));
        if (reminder.getEvent() == null || !reminder.getEvent().getId().equals(eventId)) {
            throw new BadRequestException("Reminder does not belong to event");
        }
        reminderRepository.delete(reminder);
    }

    public EventReminderResponse get(UUID eventId, UUID reminderId) {
        EventReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found"));
        if (reminder.getEvent() == null || !reminder.getEvent().getId().equals(eventId)) {
            throw new BadRequestException("Reminder does not belong to event");
        }
        return EventReminderResponse.from(reminder);
    }

    private String joinCsv(List<?> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}


