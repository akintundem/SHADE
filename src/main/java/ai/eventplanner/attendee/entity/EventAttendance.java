package ai.eventplanner.attendee.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import ai.eventplanner.common.domain.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event attendance with QR code support for check-in
 */
@Entity
@Table(name = "event_attendances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EventAttendance extends BaseEntity {
    
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status")
    private AttendanceStatus attendanceStatus = AttendanceStatus.REGISTERED;
    
    @Column(name = "registration_date")
    private LocalDateTime registrationDate;
    
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;
    
    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;
    
    @Column(name = "qr_code")
    private String qrCode;
    
    @Column(name = "qr_code_used")
    private Boolean qrCodeUsed = false;
    
    @Column(name = "qr_code_used_at")
    private LocalDateTime qrCodeUsedAt;
    
    @Column(name = "ticket_type")
    private String ticketType;
    
    @Column(name = "ticket_price")
    private Double ticketPrice;
    
    @Column(name = "registration_fee")
    private Double registrationFee;
    
    @Column(name = "dietary_restrictions", columnDefinition = "TEXT")
    private String dietaryRestrictions;
    
    @Column(name = "accessibility_needs", columnDefinition = "TEXT")
    private String accessibilityNeeds;
    
    @Column(name = "emergency_contact", columnDefinition = "TEXT")
    private String emergencyContact;
    
    @Column(name = "emergency_phone")
    private String emergencyPhone;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;
    
    @Column(name = "feedback_rating")
    private Integer feedbackRating;
    
    @Column(name = "feedback_comments", columnDefinition = "TEXT")
    private String feedbackComments;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    public EventAttendance(UUID eventId, UUID userId) {
        this.eventId = eventId;
        this.userId = userId;
        this.registrationDate = LocalDateTime.now();
    }
}
