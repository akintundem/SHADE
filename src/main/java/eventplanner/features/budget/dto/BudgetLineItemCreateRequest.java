package eventplanner.features.budget.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetLineItemCreateRequest {

    // Optional - will be set by controller from path parameter
    private UUID budgetId;

    @NotBlank
    private String category;

    private String description;

    private BigDecimal estimatedCost;

    private BigDecimal actualCost;

    private UUID vendorId;

    private Integer quantity;
}

