package ai.eventplanner.common.domain.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Budget updated event for RabbitMQ communication
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetUpdatedEvent {
    private UUID eventId;
    private UUID budgetId;
    private BigDecimal totalBudget;
    private BigDecimal totalEstimated;
    private BigDecimal totalActual;
    private BigDecimal variance;
    private BigDecimal variancePercentage;
    private String budgetStatus;
    private LocalDateTime updatedAt;
}
