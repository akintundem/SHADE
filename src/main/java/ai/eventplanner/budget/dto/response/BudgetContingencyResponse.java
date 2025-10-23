package ai.eventplanner.budget.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetContingencyResponse {
    
    private BigDecimal totalContingency;
    private BigDecimal contingencyUsed;
    private BigDecimal contingencyRemaining;
    private BigDecimal contingencyPercentage;
    private String contingencyStatus; // "SAFE", "WARNING", "CRITICAL"
    private List<ContingencyUsage> usageBreakdown;
    private String recommendations;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContingencyUsage {
        private String category;
        private BigDecimal amount;
        private String reason;
        private String date;
    }
}
