package eventplanner.features.budget.repository;

import eventplanner.features.budget.entity.BudgetLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BudgetLineItemRepository extends JpaRepository<BudgetLineItem, UUID> {
    List<BudgetLineItem> findByBudgetId(UUID budgetId);
}


