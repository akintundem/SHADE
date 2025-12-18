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
public class BudgetCategoryResponse {
    
    private UUID id;
    private UUID budgetId;
    private String name;
    private String description;
    private BigDecimal allocatedAmount;
    private BigDecimal totalEstimated;
    private BigDecimal totalActual;
    private BigDecimal remaining;
    private Integer displayOrder;
    private Integer lineItemCount;
    private List<BudgetLineItemResponse> lineItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BudgetCategoryResponse fromEntity(eventplanner.features.budget.entity.BudgetCategory entity) {
        if (entity == null) return null;
        BudgetCategoryResponse response = new BudgetCategoryResponse();
        response.setId(entity.getId());
        response.setBudgetId(entity.getBudget() != null ? entity.getBudget().getId() : null);
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setAllocatedAmount(entity.getAllocatedAmount());
        response.setTotalEstimated(entity.getTotalEstimated());
        response.setTotalActual(entity.getTotalActual());
        response.setRemaining(entity.getRemaining());
        response.setDisplayOrder(entity.getDisplayOrder());
        response.setLineItemCount(entity.getLineItems() != null ? entity.getLineItems().size() : 0);
        
        if (entity.getLineItems() != null && !entity.getLineItems().isEmpty()) {
            response.setLineItems(entity.getLineItems().stream()
                    .map(BudgetLineItemResponse::fromEntity)
                    .toList());
        }
        
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}

