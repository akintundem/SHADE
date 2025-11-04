package eventplanner.features.event.service;

import eventplanner.features.event.dto.request.EventReminderRequest;
import eventplanner.features.event.dto.response.EventReminderResponse;
import eventplanner.features.event.entity.EventReminder;
import eventplanner.features.event.repository.EventReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class EventReminderService {

    private final EventReminderRepository reminderRepository;

    public List<EventReminderResponse> list(UUID eventId, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return reminderRepository.findByEventId(eventId, pageable)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public EventReminderResponse create(UUID eventId, EventReminderRequest request) {
        EventReminder reminder = new EventReminder();
        reminder.setEventId(eventId);
        reminder.setTitle(request.getTitle());
        reminder.setDescription(request.getDescription());
        reminder.setReminderTime(request.getReminderTime());
        reminder.setChannel(request.getChannel());
        reminder.setReminderType(request.getReminderType());
        reminder.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        reminder.setCustomMessage(request.getCustomMessage());
        reminder.setRecipientUserIdsCsv(joinCsv(request.getRecipientUserIds()));
        reminder.setRecipientEmailsCsv(joinCsv(request.getRecipientEmails()));
        reminder.setWasSent(false);
        EventReminder saved = reminderRepository.save(reminder);
        return toResponse(saved);
    }

    public EventReminderResponse update(UUID eventId, UUID reminderId, EventReminderRequest request) {
        EventReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found"));
        if (!reminder.getEventId().equals(eventId)) {
            throw new IllegalArgumentException("Reminder does not belong to event");
        }
        if (request.getTitle() != null) reminder.setTitle(request.getTitle());
        if (request.getDescription() != null) reminder.setDescription(request.getDescription());
        if (request.getReminderTime() != null) reminder.setReminderTime(request.getReminderTime());
        if (request.getChannel() != null) reminder.setChannel(request.getChannel());
        if (request.getReminderType() != null) reminder.setReminderType(request.getReminderType());
        if (request.getIsActive() != null) reminder.setIsActive(request.getIsActive());
        if (request.getCustomMessage() != null) reminder.setCustomMessage(request.getCustomMessage());
        if (request.getRecipientUserIds() != null) reminder.setRecipientUserIdsCsv(joinCsv(request.getRecipientUserIds()));
        if (request.getRecipientEmails() != null) reminder.setRecipientEmailsCsv(joinCsv(request.getRecipientEmails()));
        return toResponse(reminder);
    }

    public void delete(UUID eventId, UUID reminderId) {
        EventReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found"));
        if (!reminder.getEventId().equals(eventId)) {
            throw new IllegalArgumentException("Reminder does not belong to event");
        }
        reminderRepository.delete(reminder);
    }

    public EventReminderResponse get(UUID eventId, UUID reminderId) {
        EventReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found"));
        if (!reminder.getEventId().equals(eventId)) {
            throw new IllegalArgumentException("Reminder does not belong to event");
        }
        return toResponse(reminder);
    }

    private EventReminderResponse toResponse(EventReminder r) {
        EventReminderResponse resp = new EventReminderResponse();
        resp.setReminderId(r.getId());
        resp.setEventId(r.getEventId());
        resp.setTitle(r.getTitle());
        resp.setDescription(r.getDescription());
        resp.setReminderTime(r.getReminderTime());
        resp.setChannel(r.getChannel());
        resp.setReminderType(r.getReminderType());
        resp.setIsActive(r.getIsActive());
        resp.setCustomMessage(r.getCustomMessage());
        resp.setRecipientCount(countRecipients(r));
        resp.setCreatedAt(r.getCreatedAt());
        resp.setUpdatedAt(r.getUpdatedAt());
        resp.setWasSent(r.getWasSent());
        return resp;
    }

    private int countRecipients(EventReminder r) {
        int users = r.getRecipientUserIdsCsv() == null || r.getRecipientUserIdsCsv().isBlank() ? 0 : r.getRecipientUserIdsCsv().split(",").length;
        int emails = r.getRecipientEmailsCsv() == null || r.getRecipientEmailsCsv().isBlank() ? 0 : r.getRecipientEmailsCsv().split(",").length;
        return users + emails;
    }

    private String joinCsv(List<?> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}


