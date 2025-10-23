package ai.eventplanner.budget.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBudgetRequest {
    
    @NotNull(message = "Total budget is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total budget must be greater than 0")
    private BigDecimal totalBudget;
    
    @DecimalMin(value = "0.0", message = "Contingency percentage must be non-negative")
    @DecimalMax(value = "50.0", message = "Contingency percentage should not exceed 50%")
    private BigDecimal contingencyPercentage;
    
    private String currency;
    
    private String notes;
}