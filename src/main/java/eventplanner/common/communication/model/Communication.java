package eventplanner.common.communication.model;

import eventplanner.common.domain.entity.BaseEntity;
import eventplanner.common.communication.enums.CommunicationStatus;
import eventplanner.common.communication.enums.CommunicationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Communication tracking across all channels
 */
@Entity
@Table(name = "communications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Communication extends BaseEntity {
    
    @Column(name = "event_id", nullable = true)
    private UUID eventId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "communication_type")
    private CommunicationType communicationType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type")
    private CommunicationRecipientType recipientType;
    
    @Column(name = "recipient_id")
    private UUID recipientId;
    
    @Column(name = "recipient_email")
    private String recipientEmail;
    
    @Column(name = "subject")
    private String subject;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CommunicationStatus status = CommunicationStatus.PENDING;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "failed_at")
    private LocalDateTime failedAt;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(name = "external_id")
    private String externalId; // Provider's message ID
    
    @Column(name = "template_id")
    private String templateId;
    
    @Column(name = "channel")
    private String channel; // For logging: "email", "push", "sms"
    
    public Communication(UUID eventId, CommunicationType communicationType, CommunicationRecipientType recipientType, String subject, String content) {
        this.eventId = eventId;
        this.communicationType = communicationType;
        this.recipientType = recipientType;
        this.subject = subject;
        this.content = content;
    }
}

