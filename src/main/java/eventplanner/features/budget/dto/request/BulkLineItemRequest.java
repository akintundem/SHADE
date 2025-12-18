package eventplanner.features.budget.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkLineItemRequest {
    private UUID budgetId;
    
    @NotEmpty(message = "Line items list cannot be empty")
    @Valid
    private List<BudgetLineItemAutoSaveRequest> lineItems;
}

