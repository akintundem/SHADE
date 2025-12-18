package eventplanner.features.budget.repository;

import eventplanner.features.budget.entity.BudgetLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BudgetLineItemRepository extends JpaRepository<BudgetLineItem, UUID> {
    // UUID-based query
    List<BudgetLineItem> findByBudgetId(UUID budgetId);

    @Query("SELECT SUM(li.estimatedCost) FROM BudgetLineItem li WHERE li.budget.id = :budgetId AND li.isDraft = false")
    BigDecimal sumEstimatedCostByBudgetId(@Param("budgetId") UUID budgetId);

    @Query("SELECT SUM(li.actualCost) FROM BudgetLineItem li WHERE li.budget.id = :budgetId AND li.isDraft = false")
    BigDecimal sumActualCostByBudgetId(@Param("budgetId") UUID budgetId);

    @Query("SELECT SUM(li.estimatedCost) FROM BudgetLineItem li WHERE li.budgetCategory.id = :categoryId AND li.isDraft = false")
    BigDecimal sumEstimatedCostByCategoryId(@Param("categoryId") UUID categoryId);

    @Query("SELECT SUM(li.actualCost) FROM BudgetLineItem li WHERE li.budgetCategory.id = :categoryId AND li.isDraft = false")
    BigDecimal sumActualCostByCategoryId(@Param("categoryId") UUID categoryId);
}


