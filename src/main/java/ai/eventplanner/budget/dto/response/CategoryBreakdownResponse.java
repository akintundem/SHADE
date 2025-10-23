package ai.eventplanner.budget.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBreakdownResponse {
    
    private Map<String, BigDecimal> estimatedByCategory;
    private Map<String, BigDecimal> actualByCategory;
    private Map<String, BigDecimal> varianceByCategory;
    private List<CategorySummary> categorySummaries;
    private BigDecimal totalEstimated;
    private BigDecimal totalActual;
    private BigDecimal totalVariance;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private String category;
        private BigDecimal estimatedAmount;
        private BigDecimal actualAmount;
        private BigDecimal variance;
        private BigDecimal percentageOfTotal;
        private String status; // "UNDER_BUDGET", "OVER_BUDGET", "ON_BUDGET"
    }
}
