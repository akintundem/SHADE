package ai.eventplanner.event.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event attendance with QR code support for check-in
 */
@Entity
@Table(name = "event_attendances")
public class EventAttendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private EventUser user;
    
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
    
    @Column(name = "payment_status")
    private String paymentStatus;
    
    @Column(name = "dietary_restrictions", columnDefinition = "TEXT")
    private String dietaryRestrictions;
    
    @Column(name = "accessibility_needs", columnDefinition = "TEXT")
    private String accessibilityNeeds;
    
    @Column(name = "emergency_contact", columnDefinition = "TEXT")
    private String emergencyContact;
    
    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;
    
    @Column(name = "feedback_rating")
    private Integer feedbackRating;
    
    @Column(name = "feedback_comments", columnDefinition = "TEXT")
    private String feedbackComments;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public EventAttendance() {}
    
    public EventAttendance(Event event, EventUser user) {
        this.event = event;
        this.user = user;
        this.registrationDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    
    public EventUser getUser() { return user; }
    public void setUser(EventUser user) { this.user = user; }
    
    public AttendanceStatus getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(AttendanceStatus attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }
    
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    
    public Boolean getQrCodeUsed() { return qrCodeUsed; }
    public void setQrCodeUsed(Boolean qrCodeUsed) { this.qrCodeUsed = qrCodeUsed; }
    
    public LocalDateTime getQrCodeUsedAt() { return qrCodeUsedAt; }
    public void setQrCodeUsedAt(LocalDateTime qrCodeUsedAt) { this.qrCodeUsedAt = qrCodeUsedAt; }
    
    public String getTicketType() { return ticketType; }
    public void setTicketType(String ticketType) { this.ticketType = ticketType; }
    
    public Double getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(Double ticketPrice) { this.ticketPrice = ticketPrice; }
    
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    
    public String getDietaryRestrictions() { return dietaryRestrictions; }
    public void setDietaryRestrictions(String dietaryRestrictions) { this.dietaryRestrictions = dietaryRestrictions; }
    
    public String getAccessibilityNeeds() { return accessibilityNeeds; }
    public void setAccessibilityNeeds(String accessibilityNeeds) { this.accessibilityNeeds = accessibilityNeeds; }
    
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    
    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }
    
    public Integer getFeedbackRating() { return feedbackRating; }
    public void setFeedbackRating(Integer feedbackRating) { this.feedbackRating = feedbackRating; }
    
    public String getFeedbackComments() { return feedbackComments; }
    public void setFeedbackComments(String feedbackComments) { this.feedbackComments = feedbackComments; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public enum AttendanceStatus {
        REGISTERED,
        CONFIRMED,
        CHECKED_IN,
        CHECKED_OUT,
        CANCELLED,
        NO_SHOW
    }
}
