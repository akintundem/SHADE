package eventplanner.features.budget.dto.response;

import eventplanner.features.budget.enums.PlanningStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetLineItemResponse {
    
    private UUID id;
    private UUID budgetId;
    private UUID budgetCategoryId;
    private String budgetCategoryName;
    private String subcategory;
    private String description;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private BigDecimal variance;
    private BigDecimal variancePercentage;
    private Integer quantity;
    private BigDecimal unitCost;
    private PlanningStatus planningStatus;
    private Boolean isEssential;
    private String priority;
    private String notes;
    private Boolean isDraft;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BudgetLineItemResponse fromEntity(eventplanner.features.budget.entity.BudgetLineItem entity) {
        if (entity == null) return null;
        BudgetLineItemResponse response = new BudgetLineItemResponse();
        response.setId(entity.getId());
        response.setBudgetId(entity.getBudget() != null ? entity.getBudget().getId() : null);
        response.setBudgetCategoryId(entity.getBudgetCategory() != null ? entity.getBudgetCategory().getId() : null);
        response.setBudgetCategoryName(entity.getBudgetCategory() != null ? entity.getBudgetCategory().getName() : null);
        response.setSubcategory(entity.getSubcategory());
        response.setDescription(entity.getDescription());
        response.setEstimatedCost(entity.getEstimatedCost());
        response.setActualCost(entity.getActualCost());
        response.setVariance(entity.getVariance());
        response.setVariancePercentage(entity.getVariancePercentage());
        response.setQuantity(entity.getQuantity());
        response.setUnitCost(entity.getUnitCost());
        response.setPlanningStatus(entity.getPlanningStatus());
        response.setIsEssential(entity.getIsEssential());
        response.setPriority(entity.getPriority());
        response.setNotes(entity.getNotes());
        response.setIsDraft(entity.getIsDraft());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}