package ai.eventplanner.budget.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BudgetLineItemResponse {
    
    private UUID id;
    private UUID budgetId;
    private String description;
    private BigDecimal amount;
    private String category;
}
