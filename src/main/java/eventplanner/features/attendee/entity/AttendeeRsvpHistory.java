package eventplanner.features.attendee.entity;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.features.attendee.enums.AttendeeStatus;
import eventplanner.features.attendee.enums.RsvpChangeSource;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "attendee_rsvp_history",
    indexes = {
        @Index(name = "idx_rsvp_history_event_id", columnList = "event_id"),
        @Index(name = "idx_rsvp_history_attendee_id", columnList = "attendee_id"),
        @Index(name = "idx_rsvp_history_source", columnList = "source")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AttendeeRsvpHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendee_id", nullable = false)
    private Attendee attendee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private UserAccount changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private AttendeeStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private AttendeeStatus newStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private RsvpChangeSource source = RsvpChangeSource.USER;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
