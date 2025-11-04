package eventplanner.features.budget.dto.response;

import eventplanner.common.domain.enums.PlanningStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetLineItemResponse {
    
    private UUID id;
    private UUID budgetId;
    private String category;
    private String subcategory;
    private String description;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private BigDecimal variance;
    private BigDecimal variancePercentage;
    private Integer quantity;
    private BigDecimal unitCost;
    private PlanningStatus planningStatus;
    private Boolean isEssential;
    private String priority;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}