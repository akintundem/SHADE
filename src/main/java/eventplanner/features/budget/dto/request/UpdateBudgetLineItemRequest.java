package eventplanner.features.budget.dto.request;

import eventplanner.common.domain.enums.PlanningStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBudgetLineItemRequest {
    
    @NotBlank(message = "Category is required")
    private String category;
    
    private String subcategory;
    
    private String description;
    
    @DecimalMin(value = "0.0", message = "Estimated cost must be non-negative")
    private BigDecimal estimatedCost;
    
    @DecimalMin(value = "0.0", message = "Actual cost must be non-negative")
    private BigDecimal actualCost;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    @DecimalMin(value = "0.0", message = "Unit cost must be non-negative")
    private BigDecimal unitCost;
    
    private PlanningStatus planningStatus;
    
    private Boolean isEssential;
    
    private String priority;
    
    private String notes;
}
