package eventplanner.features.budget.service;

import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.features.budget.dto.request.UpdateBudgetRequest;
import eventplanner.features.budget.dto.request.BudgetLineItemAutoSaveRequest;
import eventplanner.features.budget.entity.Budget;
import eventplanner.features.budget.entity.BudgetCategory;
import eventplanner.features.budget.entity.BudgetLineItem;
import eventplanner.features.event.entity.Event;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.features.budget.repository.BudgetCategoryRepository;
import eventplanner.features.budget.repository.BudgetLineItemRepository;
import eventplanner.features.budget.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final BudgetCategoryRepository categoryRepository;
    private final BudgetLineItemRepository lineItemRepository;

    public BudgetService(BudgetRepository budgetRepository,
                        BudgetCategoryRepository categoryRepository,
                        BudgetLineItemRepository lineItemRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.lineItemRepository = lineItemRepository;
    }

    public Optional<Budget> getByEventId(UUID eventId) {
        return budgetRepository.findByEventId(eventId);
    }

    public Budget getBudgetOrThrow(UUID eventId) {
        return getByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found for event"));
    }


    private static final List<String> STANDARD_CATEGORIES = List.of(
        "Venue & Facilities", "Catering & Food", "Marketing & Promotion",
        "Audio & Visual", "Speakers & Talent", "Decorations & Flowers",
        "Printing & Signage", "Logistics & Transport", "Staff & Security",
        "Technology & Software", "Gifts & Swag", "Insurance & Permits",
        "Photography & Video", "Registration & Admin", "Travel & Accommodation",
        "Sponsorship Activation", "Emergency & Contingency", "Equipment Rental",
        "Volunteer Support", "Awards & Trophies", "Miscellaneous"
    );

    public Budget createInitialBudget(Event event, UserAccount owner) {
        Budget budget = new Budget();
        budget.setEvent(event);
        budget.setOwner(owner);
        budget.setTotalBudget(BigDecimal.ZERO);
        budget.setCurrency("USD");
        budget.setBudgetStatus("DRAFT");
        
        Budget savedBudget = budgetRepository.save(budget);
        seedStandardCategories(savedBudget);
        return savedBudget;
    }

    private void seedStandardCategories(Budget budget) {
        int order = 0;
        for (String categoryName : STANDARD_CATEGORIES) {
            BudgetCategory category = new BudgetCategory();
            category.setBudget(budget);
            category.setName(categoryName);
            category.setAllocatedAmount(BigDecimal.ZERO);
            category.setDisplayOrder(order++);
            categoryRepository.save(category);
        }
    }

    public Budget updateBudget(UUID budgetId, UpdateBudgetRequest request) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        if (request.getTotalBudget() != null) {
            validatePositiveAmount(request.getTotalBudget(), "Total budget");
            budget.setTotalBudget(request.getTotalBudget());
        }
        if (request.getContingencyPercentage() != null) {
            validatePercentage(request.getContingencyPercentage(), "Contingency percentage");
            budget.setContingencyPercentage(request.getContingencyPercentage());
        }
        if (request.getCurrency() != null) {
            validateCurrency(request.getCurrency());
            budget.setCurrency(request.getCurrency());
        }
        if (request.getNotes() != null) {
            budget.setNotes(request.getNotes());
        }
        // Budget status is auto-calculated; ignore manual overrides to prevent
        // inconsistent state between actual spend and the status label.
        
        recalculateContingency(budget);
        budgetRepository.save(budget);
        
        // Refetch with categories to avoid lazy loading issues when converting to response
        return budgetRepository.findByEventId(budget.getEvent().getId())
                .orElse(budget);
    }

    /**
     * Auto-save a line item as draft.
     */
    public BudgetLineItem autoSaveLineItemDraft(Budget budget, BudgetLineItemAutoSaveRequest request) {
        BudgetLineItem item;
        
        if (request.getId() == null) {
            if (request.getBudgetCategoryId() == null) {
                throw new IllegalArgumentException("Budget category ID is required for new line items");
            }
            BudgetCategory category = categoryRepository.findById(request.getBudgetCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            if (!category.getBudget().getId().equals(budget.getId())) {
                throw new IllegalArgumentException("Category does not belong to this budget");
            }

            item = new BudgetLineItem();
            item.setBudget(budget);
            item.setBudgetCategory(category);
            item.setIsDraft(true);
        } else {
            // Fetch line item with category to avoid lazy loading issues
            item = lineItemRepository.findByIdWithCategory(request.getId())
                    .orElseGet(() -> lineItemRepository.findById(request.getId())
                            .orElseThrow(() -> new RuntimeException("Line item not found")));
            if (!item.getBudget().getId().equals(budget.getId())) {
                throw new IllegalArgumentException("Line item does not belong to this budget");
            }
            if (request.getBudgetCategoryId() != null && 
                (item.getBudgetCategory() == null || !item.getBudgetCategory().getId().equals(request.getBudgetCategoryId()))) {
                BudgetCategory category = categoryRepository.findById(request.getBudgetCategoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
                if (!category.getBudget().getId().equals(budget.getId())) {
                    throw new IllegalArgumentException("Category does not belong to this budget");
                }
                item.setBudgetCategory(category);
            }
        }
        
        updateLineItemFields(item, request);
        boolean wasFinalized = !Boolean.TRUE.equals(item.getIsDraft());
        if (wasFinalized) {
            calculateLineItemVariance(item);
        }
        BudgetLineItem saved = lineItemRepository.save(item);
        if (wasFinalized) {
            recalculateCategoryTotals(saved.getBudgetCategory().getId());
            recalculateBudgetTotals(saved.getBudget().getId());
        }
        return saved;
    }

    /**
     * Finalize a draft line item with auto-cleanup.
     */
    public Optional<BudgetLineItem> finalizeLineItem(Budget budget, UUID itemId, BudgetLineItemAutoSaveRequest request) {
        BudgetLineItem item = lineItemRepository.findByIdWithCategory(itemId)
                .orElseGet(() -> lineItemRepository.findById(itemId)
                        .orElseThrow(() -> new RuntimeException("Line item not found")));
        if (!item.getBudget().getId().equals(budget.getId())) {
            throw new IllegalArgumentException("Line item does not belong to this budget");
        }
        if (request.getBudgetCategoryId() != null &&
                (item.getBudgetCategory() == null || !item.getBudgetCategory().getId().equals(request.getBudgetCategoryId()))) {
            BudgetCategory category = categoryRepository.findById(request.getBudgetCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            if (!category.getBudget().getId().equals(budget.getId())) {
                throw new IllegalArgumentException("Category does not belong to this budget");
            }
            item.setBudgetCategory(category);
        }
        
        updateLineItemFields(item, request);

        if (isLineItemEmpty(item)) {
            deleteLineItem(budget, itemId);
            return Optional.empty();
        }

        calculateLineItemVariance(item);
        item.setIsDraft(false);
        BudgetLineItem saved = lineItemRepository.save(item);
        
        recalculateCategoryTotals(saved.getBudgetCategory().getId());
        recalculateBudgetTotals(saved.getBudget().getId());
        
        return Optional.of(saved);
    }

    private void updateLineItemFields(BudgetLineItem item, BudgetLineItemAutoSaveRequest request) {
        if (request.getSubcategory() != null) item.setSubcategory(request.getSubcategory());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getEstimatedCost() != null) item.setEstimatedCost(request.getEstimatedCost());
        if (request.getActualCost() != null) item.setActualCost(request.getActualCost());
        if (request.getQuantity() != null) item.setQuantity(request.getQuantity());
        if (request.getUnitCost() != null) item.setUnitCost(request.getUnitCost());
        if (request.getPlanningStatus() != null) item.setPlanningStatus(request.getPlanningStatus());
        if (request.getIsEssential() != null) item.setIsEssential(request.getIsEssential());
        if (request.getPriority() != null) item.setPriority(request.getPriority());
        if (request.getNotes() != null) item.setNotes(request.getNotes());

        // Auto-compute estimatedCost from unitCost * quantity when both are present
        // and no explicit estimatedCost was provided in this request.
        if (request.getEstimatedCost() == null
                && item.getUnitCost() != null && item.getQuantity() != null && item.getQuantity() > 0) {
            item.setEstimatedCost(item.getUnitCost().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
    }

    private boolean isLineItemEmpty(BudgetLineItem item) {
        return (item.getDescription() == null || item.getDescription().isBlank()) &&
               (item.getEstimatedCost() == null || item.getEstimatedCost().compareTo(BigDecimal.ZERO) == 0) &&
               (item.getActualCost() == null || item.getActualCost().compareTo(BigDecimal.ZERO) == 0);
    }

    private void calculateLineItemVariance(BudgetLineItem item) {
        if (item.getActualCost() != null && item.getEstimatedCost() != null) {
            item.setVariance(item.getActualCost().subtract(item.getEstimatedCost()));
            if (item.getEstimatedCost().compareTo(BigDecimal.ZERO) > 0) {
                item.setVariancePercentage(item.getVariance()
                    .divide(item.getEstimatedCost(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP));
            } else {
                item.setVariancePercentage(null);
            }
        } else {
            item.setVariance(null);
            item.setVariancePercentage(null);
        }
    }

    public void deleteLineItem(Budget budget, UUID itemId) {
        BudgetLineItem item = lineItemRepository.findByIdWithCategory(itemId)
                .orElseGet(() -> lineItemRepository.findById(itemId).orElse(null));
        if (item == null) {
            throw new eventplanner.common.exception.exceptions.ResourceNotFoundException(
                "Budget line item not found: " + itemId);
        }

        UUID budgetId = item.getBudget().getId();
        if (!budget.getId().equals(budgetId)) {
            throw new IllegalArgumentException("Line item does not belong to this budget");
        }
        UUID categoryId = item.getBudgetCategory().getId();
        boolean wasFinalized = !Boolean.TRUE.equals(item.getIsDraft());

        lineItemRepository.deleteById(itemId);
        
        if (wasFinalized) {
            recalculateCategoryTotals(categoryId);
            recalculateBudgetTotals(budgetId);
        }
    }

    public List<BudgetLineItem> listLineItems(UUID budgetId) {
        return lineItemRepository.findByBudgetId(budgetId);
    }

    public List<BudgetCategory> listCategories(UUID budgetId) {
        return categoryRepository.findByBudgetIdOrderByDisplayOrderAsc(budgetId);
    }
    
    private void recalculateCategoryTotals(UUID categoryId) {
        BudgetCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        BigDecimal totalEstimated = lineItemRepository.sumEstimatedCostByCategoryId(categoryId);
        BigDecimal totalActual = lineItemRepository.sumActualCostByCategoryId(categoryId);
        
        category.setTotalEstimated(totalEstimated != null ? totalEstimated : BigDecimal.ZERO);
        category.setTotalActual(totalActual != null ? totalActual : BigDecimal.ZERO);
        category.setRemaining(category.getAllocatedAmount().subtract(category.getTotalActual()));
        
        categoryRepository.save(category);
    }

    private void recalculateBudgetTotals(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId).orElse(null);
        if (budget == null) return;
        
        BigDecimal totalEstimated = lineItemRepository.sumEstimatedCostByBudgetId(budgetId);
        BigDecimal totalActual = lineItemRepository.sumActualCostByBudgetId(budgetId);
        
        budget.setTotalEstimated(totalEstimated != null ? totalEstimated : BigDecimal.ZERO);
        budget.setTotalActual(totalActual != null ? totalActual : BigDecimal.ZERO);
        budget.setVariance(budget.getTotalActual().subtract(budget.getTotalEstimated()));
        
        if (budget.getTotalEstimated().compareTo(BigDecimal.ZERO) > 0) {
            budget.setVariancePercentage(budget.getVariance()
                .divide(budget.getTotalEstimated(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")));
        } else {
            budget.setVariancePercentage(null);
        }
        
        recalculateContingency(budget);
        budgetRepository.save(budget);
    }

    private void recalculateContingency(Budget budget) {
        if (budget.getContingencyPercentage() != null && budget.getTotalBudget() != null) {
            budget.setContingencyAmount(budget.getTotalBudget()
                .multiply(budget.getContingencyPercentage())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        } else {
            budget.setContingencyAmount(null);
        }
    }

    // ---- Shared validation helpers (kept here since they're budget-specific) ----

    static void validatePositiveAmount(BigDecimal amount, String fieldName) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }

    static void validatePercentage(BigDecimal percentage, String fieldName) {
        if (percentage != null && (percentage.compareTo(BigDecimal.ZERO) < 0
                || percentage.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 100");
        }
    }

    static void validateCurrency(String currency) {
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Invalid currency code");
        }
    }
}
