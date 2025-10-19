package ai.eventplanner.common.domain.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Communication sent event for RabbitMQ communication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationSentEvent {
    private UUID eventId;
    private UUID communicationId;
    private String communicationType;
    private String recipientType;
    private UUID recipientId;
    private String subject;
    private String status;
    private LocalDateTime sentAt;
    private String externalId;
}
