package eventplanner.features.budget.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetUpsertRequest {
    @NotNull
    private UUID eventId;

    @NotNull
    private BigDecimal totalBudget;

    private String currency;

    private BigDecimal contingencyPercentage;

    private String notes;
}

