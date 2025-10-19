package ai.eventplanner.common.domain.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Risk detected event for RabbitMQ communication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskDetectedEvent {
    private UUID eventId;
    private UUID riskId;
    private String riskType;
    private String severity;
    private String title;
    private String description;
    private String mitigationPlan;
    private String assignedTo;
    private LocalDateTime detectedAt;
}
