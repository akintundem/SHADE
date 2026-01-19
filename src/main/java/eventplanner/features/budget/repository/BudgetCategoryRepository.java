package eventplanner.features.budget.repository;

import eventplanner.features.budget.entity.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, UUID> {
    
    // Find all categories for a budget
    List<BudgetCategory> findByBudgetIdOrderByDisplayOrderAsc(UUID budgetId);
}

