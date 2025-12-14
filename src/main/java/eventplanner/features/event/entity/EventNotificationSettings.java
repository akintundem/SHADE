package eventplanner.features.event.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "event_notification_settings",
    uniqueConstraints = @UniqueConstraint(name = "uk_event_notification_settings_event", columnNames = "event_id"))
@Getter
@Setter
public class EventNotificationSettings extends BaseEntity {

    /**
     * One-to-one relationship with the event (unique constraint ensures one settings per event).
     * This is the owning side of the relationship.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, updatable = false, unique = true)
    private Event event;

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

    public static EventNotificationSettings createDefault(Event event) {
        EventNotificationSettings settings = new EventNotificationSettings();
        settings.setEvent(event);
        return settings;
    }
}
