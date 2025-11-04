package eventplanner.features.event.entity;

import eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_reminders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventReminder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "reminder_time", nullable = false)
    private LocalDateTime reminderTime;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "reminder_type", nullable = false)
    private String reminderType;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "custom_message")
    private String customMessage;

    @Column(name = "recipient_user_ids", columnDefinition = "TEXT")
    private String recipientUserIdsCsv;

    @Column(name = "recipient_emails", columnDefinition = "TEXT")
    private String recipientEmailsCsv;

    @Column(name = "was_sent", nullable = false)
    private Boolean wasSent = false;
}


