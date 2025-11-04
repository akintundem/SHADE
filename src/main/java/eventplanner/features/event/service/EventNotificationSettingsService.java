package eventplanner.features.event.service;

import eventplanner.features.event.dto.request.EventNotificationSettingsRequest;
import eventplanner.features.event.dto.response.EventNotificationSettingsResponse;
import eventplanner.features.event.entity.EventNotificationSettings;
import eventplanner.features.event.enums.EventNotificationChannel;
import eventplanner.features.event.repository.EventNotificationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EventNotificationSettingsService {

    private static final List<EventNotificationChannel> SUPPORTED_CHANNELS = List.of(EventNotificationChannel.EMAIL, EventNotificationChannel.SMS, EventNotificationChannel.PUSH);

    private final EventNotificationSettingsRepository settingsRepository;

    public EventNotificationSettingsResponse getSettings(UUID eventId) {
        EventNotificationSettings settings = ensureSettings(eventId);
        return toResponse(settings);
    }

    public EventNotificationSettingsResponse updateSettings(UUID eventId, EventNotificationSettingsRequest request) {
        EventNotificationSettings settings = ensureSettings(eventId);
        applyChannelSettings(settings, request.getEnabledChannels());
        settings.setReminderEnabled(request.getReminderEnabled());
        settings.setDefaultReminderMinutes(request.getDefaultReminderMinutes());
        settingsRepository.save(settings);
        return toResponse(settings);
    }

    public EventNotificationSettings getSettingsEntity(UUID eventId) {
        return ensureSettings(eventId);
    }

    private EventNotificationSettings ensureSettings(UUID eventId) {
        return settingsRepository.findByEventId(eventId)
            .orElseGet(() -> settingsRepository.save(EventNotificationSettings.createDefault(eventId)));
    }

    private EventNotificationSettingsResponse toResponse(EventNotificationSettings settings) {
        EventNotificationSettingsResponse response = new EventNotificationSettingsResponse();
        response.setEventId(settings.getEventId());
        response.setEmailNotifications(settings.getEmailEnabled());
        response.setSmsNotifications(settings.getSmsEnabled());
        response.setPushNotifications(settings.getPushEnabled());
        response.setReminderEnabled(settings.getReminderEnabled());
        response.setDefaultReminderMinutes(settings.getDefaultReminderMinutes());
        response.setAvailableChannels(SUPPORTED_CHANNELS);
        response.setEnabledChannels(buildEnabledChannels(settings));
        if (settings.getUpdatedAt() != null) {
            response.setUpdatedAt(settings.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME));
        }
        response.setAvailableTemplates(List.of("event_reminder", "event_update", "event_cancelled"));
        return response;
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

    private List<EventNotificationChannel> buildEnabledChannels(EventNotificationSettings settings) {
        return SUPPORTED_CHANNELS.stream()
            .filter(channel -> switch (channel) {
                case EMAIL -> Boolean.TRUE.equals(settings.getEmailEnabled());
                case SMS -> Boolean.TRUE.equals(settings.getSmsEnabled());
                case PUSH -> Boolean.TRUE.equals(settings.getPushEnabled());
            })
            .toList();
    }
}
