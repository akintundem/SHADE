package eventplanner.features.attendee.entity;

import eventplanner.features.attendee.enums.AttendeeStatus;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Attendee entity for event participants.
 * Can be linked to a UserAccount (if user is in the platform) or just email (if external user).
 */
@Entity
@Table(name = "attendees", indexes = {
    @Index(name = "idx_attendees_event_id", columnList = "event_id"),
    @Index(name = "idx_attendees_user_id", columnList = "user_id"),
    @Index(name = "idx_attendees_email", columnList = "email"),
    @Index(name = "idx_attendees_rsvp_status", columnList = "rsvp_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /**
     * Many-to-one relationship with the event this attendee belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /**
     * Many-to-one relationship with the user account (optional - null if attendee is not in the platform).
     * If user is provided, name and email will be auto-filled from user account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "rsvp_status")
    private AttendeeStatus rsvpStatus;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;
    
    // Audit timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (rsvpStatus == null) {
            rsvpStatus = AttendeeStatus.PENDING;
        }
    }
    
    /**
     * Check if attendee is checked in
     */
    public boolean isCheckedIn() {
        return checkedInAt != null;
    }
}


