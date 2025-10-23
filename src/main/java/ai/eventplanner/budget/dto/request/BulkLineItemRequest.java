package ai.eventplanner.budget.dto.request;

import ai.eventplanner.budget.dto.BudgetLineItemCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkLineItemRequest {
    
    @NotNull(message = "Budget ID is required")
    private java.util.UUID budgetId;
    
    @NotEmpty(message = "Line items list cannot be empty")
    @Valid
    private List<BudgetLineItemCreateRequest> lineItems;
}
