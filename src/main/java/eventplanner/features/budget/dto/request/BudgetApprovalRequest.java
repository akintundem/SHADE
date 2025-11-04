package eventplanner.features.budget.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetApprovalRequest {
    
    @NotBlank(message = "Approved by is required")
    private String approvedBy;
    
    private String notes;
}
