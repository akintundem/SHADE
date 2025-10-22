package ai.eventplanner.user.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "event_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_type", nullable = false)
    private String userType; // ORGANIZER, ATTENDEE, VOLUNTEER, VENDOR, etc.

    @Column(name = "registration_status")
    private String registrationStatus; // PENDING, CONFIRMED, CANCELLED, etc.

    @Column(name = "registration_date")
    private java.time.LocalDateTime registrationDate;

    @Column(name = "check_in_time")
    private java.time.LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private java.time.LocalDateTime checkOutTime;

    @Column(name = "special_requirements")
    private String specialRequirements;

    @Column(name = "dietary_restrictions")
    private String dietaryRestrictions;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "is_volunteer", nullable = false)
    private Boolean isVolunteer = false;

    @Column(name = "volunteer_hours")
    private Integer volunteerHours;
}