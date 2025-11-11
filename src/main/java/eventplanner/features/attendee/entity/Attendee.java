package eventplanner.features.attendee.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Attendee entity for event participants
 * Uses AttendeeStatus enum for type-safe RSVP status handling
 */
@Entity
@Table(name = "attendees", indexes = {
    @Index(name = "idx_attendees_event_id", columnList = "event_id"),
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

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "rsvp_status")
    private AttendeeStatus rsvpStatus;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;
    
    // Consent flags for privacy compliance (GDPR, etc.)
    @Column(name = "email_consent")
    private Boolean emailConsent = false;
    
    @Column(name = "sms_consent")
    private Boolean smsConsent = false;
    
    @Column(name = "data_processing_consent")
    private Boolean dataProcessingConsent = false;
    
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
        // Default consent to false if not set
        if (emailConsent == null) emailConsent = false;
        if (smsConsent == null) smsConsent = false;
        if (dataProcessingConsent == null) dataProcessingConsent = false;
    }
    
    /**
     * Check if attendee is checked in
     */
    public boolean isCheckedIn() {
        return checkedInAt != null;
    }
}


