package eventplanner.features.budget.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetSummaryResponse {
    
    private BigDecimal totalBudget;
    private BigDecimal totalEstimated;
    private BigDecimal totalActual;
    private BigDecimal remainingBudget;
    private BigDecimal contingencyAmount;
    private BigDecimal contingencyUsed;
    private BigDecimal contingencyRemaining;
    private BigDecimal variance;
    private BigDecimal variancePercentage;
    private String budgetStatus;
    private BigDecimal spentPercentage;
    private BigDecimal estimatedPercentage;
    private Map<String, BigDecimal> categoryBreakdown;
    private String riskLevel; // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    private String recommendations;
}
