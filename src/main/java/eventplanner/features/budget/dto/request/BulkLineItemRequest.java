package eventplanner.features.budget.dto.request;

import eventplanner.features.budget.dto.BudgetLineItemCreateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkLineItemRequest {
    
    // Optional - will be set by controller from path parameter
    private java.util.UUID budgetId;
    
    @NotEmpty(message = "Line items list cannot be empty")
    @Valid
    private List<BudgetLineItemCreateRequest> lineItems;
}
