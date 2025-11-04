package eventplanner.features.budget.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetDetailResponse {
    
    private UUID id;
    private UUID eventId;
    private BigDecimal totalBudget;
    private String currency;
    private BigDecimal contingencyPercentage;
    private BigDecimal contingencyAmount;
    private BigDecimal totalEstimated;
    private BigDecimal totalActual;
    private BigDecimal variance;
    private BigDecimal variancePercentage;
    private String budgetStatus;
    private String approvedBy;
    private LocalDateTime approvedDate;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BudgetLineItemResponse> lineItems;
}
