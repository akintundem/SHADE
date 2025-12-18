package eventplanner.features.budget.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetDetailResponse {
    
    private UUID id;
    private UUID eventId;
    private BigDecimal totalBudget;
    private String currency;
    private BigDecimal contingencyPercentage;
    private BigDecimal contingencyAmount;
    private BigDecimal totalEstimated;
    private BigDecimal totalActual;
    private BigDecimal variance;
    private BigDecimal variancePercentage;
    private String budgetStatus;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BudgetCategoryResponse> categories;

    public static BudgetDetailResponse fromEntity(eventplanner.features.budget.entity.Budget entity) {
        if (entity == null) return null;
        BudgetDetailResponse response = new BudgetDetailResponse();
        response.setId(entity.getId());
        response.setEventId(entity.getEvent() != null ? entity.getEvent().getId() : null);
        response.setTotalBudget(entity.getTotalBudget());
        response.setCurrency(entity.getCurrency());
        response.setContingencyPercentage(entity.getContingencyPercentage());
        response.setContingencyAmount(entity.getContingencyAmount());
        response.setTotalEstimated(entity.getTotalEstimated());
        response.setTotalActual(entity.getTotalActual());
        response.setVariance(entity.getVariance());
        response.setVariancePercentage(entity.getVariancePercentage());
        response.setBudgetStatus(entity.getBudgetStatus());
        response.setNotes(entity.getNotes());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        
        if (entity.getCategories() != null) {
            response.setCategories(entity.getCategories().stream()
                    .map(BudgetCategoryResponse::fromEntity)
                    .toList());
        }
        
        return response;
    }
}
