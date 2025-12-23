package eventplanner.features.budget.repository;

import eventplanner.features.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    // UUID-based query that handles multiple results by returning the most recent one
    // This is a workaround for cases where duplicate budgets exist (shouldn't happen but can occur)
    // The @SQLRestriction should filter deleted records, but we explicitly check deletedAt for safety
    @Query("SELECT b FROM Budget b WHERE b.event.id = :eventId AND b.deletedAt IS NULL ORDER BY b.createdAt DESC, b.id DESC")
    List<Budget> findAllByEventIdOrderByCreatedAtDesc(@Param("eventId") UUID eventId);
    
    // Legacy method - returns the first (most recent) budget, handling multiple results gracefully
    default Optional<Budget> findByEventId(UUID eventId) {
        List<Budget> budgets = findAllByEventIdOrderByCreatedAtDesc(eventId);
        return budgets.isEmpty() ? Optional.empty() : Optional.of(budgets.get(0));
    }
}


