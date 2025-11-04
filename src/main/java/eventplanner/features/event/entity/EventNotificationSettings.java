package eventplanner.features.event.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "event_notification_settings",
    uniqueConstraints = @UniqueConstraint(name = "uk_event_notification_settings_event", columnNames = "event_id"))
@Getter
@Setter
public class EventNotificationSettings extends BaseEntity {

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    private Boolean smsEnabled = false;

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    @Column(name = "reminder_enabled", nullable = false)
    private Boolean reminderEnabled = true;

    @Column(name = "default_reminder_minutes", nullable = false)
    private Integer defaultReminderMinutes = 1440;

    public static EventNotificationSettings createDefault(UUID eventId) {
        EventNotificationSettings settings = new EventNotificationSettings();
        settings.setEventId(eventId);
        return settings;
    }
}
