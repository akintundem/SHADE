package eventplanner.features.budget.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetVarianceAnalysisResponse {
    
    private BigDecimal totalVariance;
    private BigDecimal totalVariancePercentage;
    private List<CategoryVariance> categoryVariances;
    private Map<String, BigDecimal> topVarianceCategories;
    private String analysisSummary;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryVariance {
        private String category;
        private BigDecimal estimatedAmount;
        private BigDecimal actualAmount;
        private BigDecimal variance;
        private BigDecimal variancePercentage;
        private String status; // "UNDER_BUDGET", "OVER_BUDGET", "ON_BUDGET"
    }
}
