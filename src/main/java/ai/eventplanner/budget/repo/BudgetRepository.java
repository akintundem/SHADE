package ai.eventplanner.budget.repo;

import ai.eventplanner.budget.model.BudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<BudgetEntity, UUID> {
    Optional<BudgetEntity> findByEventId(UUID eventId);
}


