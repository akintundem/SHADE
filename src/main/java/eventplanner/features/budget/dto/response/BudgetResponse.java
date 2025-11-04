package eventplanner.features.budget.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BudgetResponse {
    
    private UUID id;
    private UUID eventId;
    private BigDecimal totalBudget;
    private String currency;
}
