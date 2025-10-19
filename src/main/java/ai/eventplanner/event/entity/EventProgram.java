package ai.eventplanner.event.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event program and content management
 */
@Entity
@Table(name = "event_programs")
public class EventProgram {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "program_type")
    private ProgramType programType;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "duration_minutes")
    private Integer durationMinutes;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "speaker_name")
    private String speakerName;
    
    @Column(name = "speaker_title")
    private String speakerTitle;
    
    @Column(name = "speaker_bio", columnDefinition = "TEXT")
    private String speakerBio;
    
    @Column(name = "speaker_email")
    private String speakerEmail;
    
    @Column(name = "speaker_phone")
    private String speakerPhone;
    
    @Column(name = "presentation_title")
    private String presentationTitle;
    
    @Column(name = "presentation_description", columnDefinition = "TEXT")
    private String presentationDescription;
    
    @Column(name = "presentation_url")
    private String presentationUrl;
    
    @Column(name = "handouts_url")
    private String handoutsUrl;
    
    @Column(name = "equipment_required", columnDefinition = "TEXT")
    private String equipmentRequired;
    
    @Column(name = "special_requirements", columnDefinition = "TEXT")
    private String specialRequirements;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "is_keynote")
    private Boolean isKeynote = false;
    
    @Column(name = "is_breakout")
    private Boolean isBreakout = false;
    
    @Column(name = "max_attendees")
    private Integer maxAttendees;
    
    @Column(name = "current_attendees")
    private Integer currentAttendees = 0;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public EventProgram() {}
    
    public EventProgram(Event event, String title, ProgramType programType, LocalDateTime startTime, Integer durationMinutes) {
        this.event = event;
        this.title = title;
        this.programType = programType;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.endTime = startTime.plusMinutes(durationMinutes);
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public ProgramType getProgramType() { return programType; }
    public void setProgramType(ProgramType programType) { this.programType = programType; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getSpeakerName() { return speakerName; }
    public void setSpeakerName(String speakerName) { this.speakerName = speakerName; }
    
    public String getSpeakerTitle() { return speakerTitle; }
    public void setSpeakerTitle(String speakerTitle) { this.speakerTitle = speakerTitle; }
    
    public String getSpeakerBio() { return speakerBio; }
    public void setSpeakerBio(String speakerBio) { this.speakerBio = speakerBio; }
    
    public String getSpeakerEmail() { return speakerEmail; }
    public void setSpeakerEmail(String speakerEmail) { this.speakerEmail = speakerEmail; }
    
    public String getSpeakerPhone() { return speakerPhone; }
    public void setSpeakerPhone(String speakerPhone) { this.speakerPhone = speakerPhone; }
    
    public String getPresentationTitle() { return presentationTitle; }
    public void setPresentationTitle(String presentationTitle) { this.presentationTitle = presentationTitle; }
    
    public String getPresentationDescription() { return presentationDescription; }
    public void setPresentationDescription(String presentationDescription) { this.presentationDescription = presentationDescription; }
    
    public String getPresentationUrl() { return presentationUrl; }
    public void setPresentationUrl(String presentationUrl) { this.presentationUrl = presentationUrl; }
    
    public String getHandoutsUrl() { return handoutsUrl; }
    public void setHandoutsUrl(String handoutsUrl) { this.handoutsUrl = handoutsUrl; }
    
    public String getEquipmentRequired() { return equipmentRequired; }
    public void setEquipmentRequired(String equipmentRequired) { this.equipmentRequired = equipmentRequired; }
    
    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public Boolean getIsKeynote() { return isKeynote; }
    public void setIsKeynote(Boolean isKeynote) { this.isKeynote = isKeynote; }
    
    public Boolean getIsBreakout() { return isBreakout; }
    public void setIsBreakout(Boolean isBreakout) { this.isBreakout = isBreakout; }
    
    public Integer getMaxAttendees() { return maxAttendees; }
    public void setMaxAttendees(Integer maxAttendees) { this.maxAttendees = maxAttendees; }
    
    public Integer getCurrentAttendees() { return currentAttendees; }
    public void setCurrentAttendees(Integer currentAttendees) { this.currentAttendees = currentAttendees; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public enum ProgramType {
        KEYNOTE,
        PRESENTATION,
        PANEL_DISCUSSION,
        WORKSHOP,
        BREAKOUT_SESSION,
        NETWORKING,
        BREAK,
        MEAL,
        ENTERTAINMENT,
        AWARDS,
        CLOSING,
        OTHER
    }
}
