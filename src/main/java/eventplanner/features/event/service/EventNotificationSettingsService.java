package eventplanner.features.event.service;

import eventplanner.features.event.dto.request.EventNotificationSettingsRequest;
import eventplanner.features.event.dto.response.EventNotificationSettingsResponse;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.entity.EventNotificationSettings;
import eventplanner.features.event.enums.EventNotificationChannel;
import eventplanner.features.event.repository.EventNotificationSettingsRepository;
import eventplanner.features.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EventNotificationSettingsService {

    private final EventNotificationSettingsRepository settingsRepository;
    private final EventRepository eventRepository;

    public EventNotificationSettingsResponse getSettings(UUID eventId) {
        EventNotificationSettings settings = ensureSettings(eventId);
        return EventNotificationSettingsResponse.from(settings);
    }

    public EventNotificationSettingsResponse updateSettings(UUID eventId, EventNotificationSettingsRequest request) {
        EventNotificationSettings settings = ensureSettings(eventId);
        applyChannelSettings(settings, request.getEnabledChannels());
        settings.setReminderEnabled(request.getReminderEnabled());
        settings.setDefaultReminderMinutes(request.getDefaultReminderMinutes());
        settingsRepository.save(settings);
        return EventNotificationSettingsResponse.from(settings);
    }

    public EventNotificationSettings getSettingsEntity(UUID eventId) {
        return ensureSettings(eventId);
    }

    private EventNotificationSettings ensureSettings(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        return settingsRepository.findByEventId(eventId)
            .orElseGet(() -> settingsRepository.save(EventNotificationSettings.createDefault(event)));
    }

    private void applyChannelSettings(EventNotificationSettings settings, Set<EventNotificationChannel> enabledChannels) {
        if (enabledChannels == null) {
            throw new IllegalArgumentException("Enabled channels must be provided");
        }
        EnumSet<EventNotificationChannel> channels = EnumSet.copyOf(enabledChannels);
        settings.setEmailEnabled(channels.contains(EventNotificationChannel.EMAIL));
        settings.setSmsEnabled(channels.contains(EventNotificationChannel.SMS));
        settings.setPushEnabled(channels.contains(EventNotificationChannel.PUSH));
    }
}
