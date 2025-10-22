package ai.eventplanner.budget.service;

import ai.eventplanner.budget.model.BudgetEntity;
import ai.eventplanner.budget.model.BudgetLineItemEntity;
import ai.eventplanner.budget.repo.BudgetLineItemRepository;
import ai.eventplanner.budget.repo.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final BudgetLineItemRepository lineItemRepository;

    public BudgetService(BudgetRepository budgetRepository, BudgetLineItemRepository lineItemRepository) {
        this.budgetRepository = budgetRepository;
        this.lineItemRepository = lineItemRepository;
    }

    public Optional<BudgetEntity> getByEventId(UUID eventId) {
        return budgetRepository.findByEventId(eventId);
    }

    public BudgetEntity createOrUpdate(BudgetEntity budget) {
        return budgetRepository.save(budget);
    }

    public BudgetLineItemEntity addLineItem(BudgetLineItemEntity item) {
        return lineItemRepository.save(item);
    }

    public List<BudgetLineItemEntity> listLineItems(UUID budgetId) {
        return lineItemRepository.findByBudgetId(budgetId);
    }

    public BigDecimal computeRollup(UUID budgetId) {
        return listLineItems(budgetId).stream()
                .map(it -> it.getActualCost() != null ? it.getActualCost() : (it.getEstimatedCost() != null ? it.getEstimatedCost() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}


