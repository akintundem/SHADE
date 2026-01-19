package eventplanner.features.budget.repository;

import eventplanner.features.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Budget persistence operations with deterministic newest-first retrieval.
 */
public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    // Order newest-first so accidental duplicate budgets resolve deterministically, and guard against soft-deleted rows.
    // Use LEFT JOIN FETCH to eagerly load event and categories to avoid LazyInitializationException
    // Note: Cannot fetch lineItems here due to MultipleBagFetchException (Hibernate limitation with multiple List collections)
    // Line items will be loaded separately when needed via BudgetService.listLineItems()
    @Query("SELECT DISTINCT b FROM Budget b " +
           "LEFT JOIN FETCH b.event " +
           "LEFT JOIN FETCH b.categories " +
           "WHERE b.event.id = :eventId AND b.deletedAt IS NULL " +
           "ORDER BY b.createdAt DESC, b.id DESC")
    List<Budget> findAllByEventIdOrderByCreatedAtDesc(@Param("eventId") UUID eventId);
    
    // Legacy method - returns the first (most recent) budget, handling multiple results gracefully
    default Optional<Budget> findByEventId(UUID eventId) {
        List<Budget> budgets = findAllByEventIdOrderByCreatedAtDesc(eventId);
        return budgets.isEmpty() ? Optional.empty() : Optional.of(budgets.get(0));
    }
}
