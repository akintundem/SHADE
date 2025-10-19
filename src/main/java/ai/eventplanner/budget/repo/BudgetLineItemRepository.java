package ai.eventplanner.budget.repo;

import ai.eventplanner.budget.model.BudgetLineItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BudgetLineItemRepository extends JpaRepository<BudgetLineItemEntity, UUID> {
    List<BudgetLineItemEntity> findByBudgetId(UUID budgetId);
}


