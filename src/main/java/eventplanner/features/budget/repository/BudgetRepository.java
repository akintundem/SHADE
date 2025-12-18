package eventplanner.features.budget.repository;

import eventplanner.features.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    // UUID-based query (for backward compatibility)
    Optional<Budget> findByEventId(UUID eventId);
}


