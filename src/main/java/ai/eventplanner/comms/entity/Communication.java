package ai.eventplanner.comms.entity;

import ai.eventplanner.common.domain.entity.BaseEntity;
import ai.eventplanner.common.domain.enums.CommunicationStatus;
import ai.eventplanner.common.domain.enums.CommunicationType;
import ai.eventplanner.common.domain.enums.RecipientType;
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
    
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "communication_type")
    private CommunicationType communicationType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type")
    private RecipientType recipientType;
    
    @Column(name = "recipient_id")
    private UUID recipientId;
    
    @Column(name = "recipient_email")
    private String recipientEmail;
    
    @Column(name = "recipient_phone")
    private String recipientPhone;
    
    @Column(name = "subject")
    private String subject;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CommunicationStatus status = CommunicationStatus.PENDING;
    
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "opened_at")
    private LocalDateTime openedAt;
    
    @Column(name = "clicked_at")
    private LocalDateTime clickedAt;
    
    @Column(name = "failed_at")
    private LocalDateTime failedAt;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(name = "external_id")
    private String externalId; // Provider's message ID
    
    @Column(name = "template_id")
    private String templateId;
    
    @Column(name = "campaign_id")
    private String campaignId;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    public Communication(UUID eventId, CommunicationType communicationType, RecipientType recipientType, String subject, String content) {
        this.eventId = eventId;
        this.communicationType = communicationType;
        this.recipientType = recipientType;
        this.subject = subject;
        this.content = content;
    }
}
