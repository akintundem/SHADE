package ai.eventplanner.budget.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateBudgetRequest {
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Total budget must be greater than 0")
    private BigDecimal totalBudget;
    
    @Size(max = 3, message = "Currency code must not exceed 3 characters")
    private String currency;
}
