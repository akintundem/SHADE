package eventplanner.features.budget.service;

import eventplanner.common.audit.service.AuditLogService;
import eventplanner.common.domain.enums.ActionType;
import eventplanner.features.budget.dto.request.UpdateBudgetRequest;
import eventplanner.features.budget.dto.request.UpdateBudgetLineItemRequest;
import eventplanner.features.budget.dto.request.BudgetApprovalRequest;
import eventplanner.features.budget.dto.request.BulkLineItemRequest;
import eventplanner.features.budget.dto.response.*;
import eventplanner.features.budget.entity.Budget;
import eventplanner.features.budget.entity.BudgetLineItem;
import eventplanner.features.budget.repository.BudgetLineItemRepository;
import eventplanner.features.budget.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final BudgetLineItemRepository lineItemRepository;
    private final AuditLogService auditLogService;

    public BudgetService(BudgetRepository budgetRepository, 
                        BudgetLineItemRepository lineItemRepository,
                        AuditLogService auditLogService) {
        this.budgetRepository = budgetRepository;
        this.lineItemRepository = lineItemRepository;
        this.auditLogService = auditLogService;
    }

    public Optional<Budget> getByEventId(UUID eventId) {
        return budgetRepository.findByEventId(eventId);
    }

    public Optional<Budget> getById(UUID budgetId) {
        return budgetRepository.findById(budgetId);
    }

    public Budget createOrUpdate(Budget budget) {
        validateBudget(budget);
        
        boolean isNew = budget.getId() == null;
        Budget oldBudget = null;
        
        if (!isNew) {
            oldBudget = budgetRepository.findById(budget.getId()).orElse(null);
        }
        
        if (budget.getContingencyPercentage() != null && budget.getTotalBudget() != null) {
            budget.setContingencyAmount(budget.getTotalBudget().multiply(budget.getContingencyPercentage()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        }
        
        Budget saved = budgetRepository.save(budget);
        
        // Audit log with unified audit system
        if (isNew) {
            auditLogService.builder()
                .domain("BUDGET")
                .entityType("Budget")
                .entityId(saved.getId())
                .action(ActionType.CREATE)
                .user(saved.getOwnerId(), null, null)
                .description(String.format("Budget created with total: %s %s", saved.getTotalBudget(), saved.getCurrency()))
                .changes(null, saved)
                .eventId(saved.getEventId())
                .log();
        } else {
            auditLogService.builder()
                .domain("BUDGET")
                .entityType("Budget")
                .entityId(saved.getId())
                .action(ActionType.UPDATE)
                .user(saved.getOwnerId(), null, null)
                .description("Budget updated")
                .changes(oldBudget, saved)
                .eventId(saved.getEventId())
                .log();
        }
        
        return saved;
    }

    public Budget updateBudget(UUID budgetId, UpdateBudgetRequest request) {
        Budget oldBudget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        // Create a snapshot of old values
        Budget oldSnapshot = new Budget();
        oldSnapshot.setTotalBudget(oldBudget.getTotalBudget());
        oldSnapshot.setContingencyPercentage(oldBudget.getContingencyPercentage());
        oldSnapshot.setCurrency(oldBudget.getCurrency());
        oldSnapshot.setNotes(oldBudget.getNotes());
        
        if (request.getTotalBudget() != null) {
            validatePositiveAmount(request.getTotalBudget(), "Total budget");
            oldBudget.setTotalBudget(request.getTotalBudget());
        }
        if (request.getContingencyPercentage() != null) {
            validatePercentage(request.getContingencyPercentage(), "Contingency percentage");
            oldBudget.setContingencyPercentage(request.getContingencyPercentage());
        }
        if (request.getCurrency() != null) {
            validateCurrency(request.getCurrency());
            oldBudget.setCurrency(request.getCurrency());
        }
        if (request.getNotes() != null) {
            oldBudget.setNotes(request.getNotes());
        }
        
        // Recalculate contingency amount
        if (oldBudget.getContingencyPercentage() != null && oldBudget.getTotalBudget() != null) {
            oldBudget.setContingencyAmount(oldBudget.getTotalBudget().multiply(oldBudget.getContingencyPercentage()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        }
        
        Budget saved = budgetRepository.save(oldBudget);
        
        // Audit log with unified audit system
        auditLogService.builder()
            .domain("BUDGET")
            .entityType("Budget")
            .entityId(saved.getId())
            .action(ActionType.UPDATE)
            .user(saved.getOwnerId(), null, null)
            .description("Budget details updated")
            .changes(oldSnapshot, saved)
            .eventId(saved.getEventId())
            .log();
        
        return saved;
    }

    public BudgetLineItem addLineItem(BudgetLineItem item) {
        BudgetLineItem saved = lineItemRepository.save(item);
        recalculateBudgetTotals(item.getBudgetId());
        return saved;
    }

    public BudgetLineItem updateLineItem(UUID itemId, UpdateBudgetLineItemRequest request) {
        BudgetLineItem item = lineItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Line item not found"));
        
        if (request.getCategory() != null) item.setCategory(request.getCategory());
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
        
        // Calculate variance
        if (item.getActualCost() != null && item.getEstimatedCost() != null) {
            item.setVariance(item.getActualCost().subtract(item.getEstimatedCost()));
            if (item.getEstimatedCost().compareTo(BigDecimal.ZERO) > 0) {
                item.setVariancePercentage(item.getVariance().divide(item.getEstimatedCost(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")));
            }
        }
        
        BudgetLineItem saved = lineItemRepository.save(item);
        recalculateBudgetTotals(item.getBudgetId());
        return saved;
    }

    public void deleteLineItem(UUID itemId) {
        BudgetLineItem item = lineItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Line item not found"));
        UUID budgetId = item.getBudgetId();
        lineItemRepository.deleteById(itemId);
        recalculateBudgetTotals(budgetId);
    }

    public List<BudgetLineItem> addBulkLineItems(BulkLineItemRequest request) {
        List<BudgetLineItem> items = request.getLineItems().stream()
                .map(req -> {
                    BudgetLineItem item = new BudgetLineItem();
                    item.setBudgetId(request.getBudgetId());
                    item.setCategory(req.getCategory());
                    item.setDescription(req.getDescription());
                    item.setEstimatedCost(req.getEstimatedCost());
                    item.setActualCost(req.getActualCost());
                    item.setVendorId(req.getVendorId());
                    return item;
                })
                .collect(Collectors.toList());
        
        List<BudgetLineItem> saved = lineItemRepository.saveAll(items);
        recalculateBudgetTotals(request.getBudgetId());
        return saved;
    }

    public List<BudgetLineItem> listLineItems(UUID budgetId) {
        return lineItemRepository.findByBudgetId(budgetId);
    }

    public Optional<BudgetLineItem> getLineItem(UUID itemId) {
        return lineItemRepository.findById(itemId);
    }

    public BigDecimal computeRollup(UUID budgetId) {
        return listLineItems(budgetId).stream()
                .map(it -> it.getActualCost() != null ? it.getActualCost() : (it.getEstimatedCost() != null ? it.getEstimatedCost() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Budget approveBudget(UUID budgetId, BudgetApprovalRequest request) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        String oldStatus = budget.getBudgetStatus();
        
        if (!"DRAFT".equals(budget.getBudgetStatus()) && !"SUBMITTED".equals(budget.getBudgetStatus())) {
            throw new RuntimeException("Budget cannot be approved in current status: " + budget.getBudgetStatus());
        }
        
        budget.setBudgetStatus("APPROVED");
        budget.setApprovedBy(request.getApprovedBy());
        budget.setApprovedDate(LocalDateTime.now());
        if (request.getNotes() != null) {
            budget.setNotes(request.getNotes());
        }
        
        Budget saved = budgetRepository.save(budget);
        
        // Audit log with unified audit system
        auditLogService.builder()
            .domain("BUDGET")
            .entityType("Budget")
            .entityId(budgetId)
            .action(ActionType.APPROVE)
            .user(budget.getOwnerId(), request.getApprovedBy(), null)
            .description(String.format("Budget approved. Status changed from %s to APPROVED", oldStatus))
            .eventId(budget.getEventId())
            .log();
        
        return saved;
    }

    public Budget rejectBudget(UUID budgetId, BudgetApprovalRequest request) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        String oldStatus = budget.getBudgetStatus();
        
        if (!"SUBMITTED".equals(budget.getBudgetStatus())) {
            throw new RuntimeException("Budget cannot be rejected in current status: " + budget.getBudgetStatus());
        }
        
        budget.setBudgetStatus("REJECTED");
        budget.setApprovedBy(request.getApprovedBy());
        budget.setApprovedDate(LocalDateTime.now());
        if (request.getNotes() != null) {
            budget.setNotes(request.getNotes());
        }
        
        Budget saved = budgetRepository.save(budget);
        
        // Audit log with unified audit system
        auditLogService.builder()
            .domain("BUDGET")
            .entityType("Budget")
            .entityId(budgetId)
            .action(ActionType.REJECT)
            .user(budget.getOwnerId(), request.getApprovedBy(), null)
            .description(String.format("Budget rejected. Status changed from %s to REJECTED. Reason: %s", 
                oldStatus, request.getNotes() != null ? request.getNotes() : "No reason provided"))
            .eventId(budget.getEventId())
            .log();
        
        return saved;
    }

    public Budget submitForApproval(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        if (!"DRAFT".equals(budget.getBudgetStatus())) {
            throw new RuntimeException("Budget cannot be submitted in current status: " + budget.getBudgetStatus());
        }
        
        budget.setBudgetStatus("SUBMITTED");
        Budget saved = budgetRepository.save(budget);
        
        // Audit log with unified audit system
        auditLogService.builder()
            .domain("BUDGET")
            .entityType("Budget")
            .entityId(budgetId)
            .action(ActionType.SUBMIT)
            .user(budget.getOwnerId(), null, null)
            .description("Budget submitted for approval")
            .eventId(budget.getEventId())
            .log();
        
        return saved;
    }

    public BudgetSummaryResponse getBudgetSummary(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        List<BudgetLineItem> lineItems = listLineItems(budgetId);
        
        BigDecimal totalEstimated = lineItems.stream()
                .map(item -> item.getEstimatedCost() != null ? item.getEstimatedCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalActual = lineItems.stream()
                .map(item -> item.getActualCost() != null ? item.getActualCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal remainingBudget = budget.getTotalBudget().subtract(totalActual);
        BigDecimal contingencyUsed = budget.getContingencyAmount() != null ? 
                budget.getContingencyAmount().subtract(remainingBudget) : BigDecimal.ZERO;
        
        BigDecimal variance = totalActual.subtract(totalEstimated);
        BigDecimal variancePercentage = totalEstimated.compareTo(BigDecimal.ZERO) > 0 ? 
                variance.divide(totalEstimated, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) : BigDecimal.ZERO;
        
        BigDecimal spentPercentage = budget.getTotalBudget().compareTo(BigDecimal.ZERO) > 0 ? 
                totalActual.divide(budget.getTotalBudget(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) : BigDecimal.ZERO;
        
        String riskLevel = determineRiskLevel(spentPercentage, variancePercentage);
        
        return new BudgetSummaryResponse(
                budget.getTotalBudget(),
                totalEstimated,
                totalActual,
                remainingBudget,
                budget.getContingencyAmount(),
                contingencyUsed,
                budget.getContingencyAmount() != null ? budget.getContingencyAmount().subtract(contingencyUsed) : BigDecimal.ZERO,
                variance,
                variancePercentage,
                budget.getBudgetStatus(),
                spentPercentage,
                spentPercentage,
                getCategoryBreakdown(lineItems),
                riskLevel,
                generateRecommendations(variance, spentPercentage, riskLevel)
        );
    }

    public BudgetVarianceAnalysisResponse getVarianceAnalysis(UUID budgetId) {
        List<BudgetLineItem> lineItems = listLineItems(budgetId);
        
        List<BudgetVarianceAnalysisResponse.CategoryVariance> categoryVariances = lineItems.stream()
                .collect(Collectors.groupingBy(BudgetLineItem::getCategory))
                .entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<BudgetLineItem> items = entry.getValue();
                    
                    BigDecimal estimated = items.stream()
                            .map(item -> item.getEstimatedCost() != null ? item.getEstimatedCost() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    BigDecimal actual = items.stream()
                            .map(item -> item.getActualCost() != null ? item.getActualCost() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    BigDecimal variance = actual.subtract(estimated);
                    BigDecimal variancePercentage = estimated.compareTo(BigDecimal.ZERO) > 0 ? 
                            variance.divide(estimated, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) : BigDecimal.ZERO;
                    
                    String status = variance.compareTo(BigDecimal.ZERO) > 0 ? "OVER_BUDGET" : 
                                  variance.compareTo(BigDecimal.ZERO) < 0 ? "UNDER_BUDGET" : "ON_BUDGET";
                    
                    return new BudgetVarianceAnalysisResponse.CategoryVariance(category, estimated, actual, variance, variancePercentage, status);
                })
                .sorted((a, b) -> b.getVariance().compareTo(a.getVariance()))
                .collect(Collectors.toList());
        
        BigDecimal totalVariance = categoryVariances.stream()
                .map(BudgetVarianceAnalysisResponse.CategoryVariance::getVariance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalEstimated = categoryVariances.stream()
                .map(BudgetVarianceAnalysisResponse.CategoryVariance::getEstimatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalVariancePercentage = totalEstimated.compareTo(BigDecimal.ZERO) > 0 ? 
                totalVariance.divide(totalEstimated, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) : BigDecimal.ZERO;
        
        Map<String, BigDecimal> topVarianceCategories = categoryVariances.stream()
                .limit(5)
                .collect(Collectors.toMap(
                        BudgetVarianceAnalysisResponse.CategoryVariance::getCategory,
                        BudgetVarianceAnalysisResponse.CategoryVariance::getVariance
                ));
        
        return new BudgetVarianceAnalysisResponse(
                totalVariance,
                totalVariancePercentage,
                categoryVariances,
                topVarianceCategories,
                generateVarianceAnalysisSummary(totalVariance, categoryVariances)
        );
    }

    public BudgetContingencyResponse getContingencyAnalysis(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        List<BudgetLineItem> lineItems = listLineItems(budgetId);
        BigDecimal totalActual = lineItems.stream()
                .map(item -> item.getActualCost() != null ? item.getActualCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalEstimated = lineItems.stream()
                .map(item -> item.getEstimatedCost() != null ? item.getEstimatedCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal overBudgetAmount = totalActual.subtract(totalEstimated);
        BigDecimal contingencyUsed = overBudgetAmount.max(BigDecimal.ZERO);
        BigDecimal contingencyRemaining = budget.getContingencyAmount() != null ? 
                budget.getContingencyAmount().subtract(contingencyUsed) : BigDecimal.ZERO;
        
        String contingencyStatus = determineContingencyStatus(contingencyRemaining, budget.getContingencyAmount());
        
        // Create usage breakdown by category for over-budget items
        List<BudgetContingencyResponse.ContingencyUsage> usageBreakdown = lineItems.stream()
                .filter(item -> {
                    BigDecimal variance = BigDecimal.ZERO;
                    if (item.getActualCost() != null && item.getEstimatedCost() != null) {
                        variance = item.getActualCost().subtract(item.getEstimatedCost());
                    }
                    return variance.compareTo(BigDecimal.ZERO) > 0;
                })
                .collect(Collectors.groupingBy(BudgetLineItem::getCategory))
                .entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<BudgetLineItem> categoryItems = entry.getValue();
                    BigDecimal categoryOverage = categoryItems.stream()
                            .map(item -> {
                                BigDecimal actual = item.getActualCost() != null ? item.getActualCost() : BigDecimal.ZERO;
                                BigDecimal estimated = item.getEstimatedCost() != null ? item.getEstimatedCost() : BigDecimal.ZERO;
                                return actual.subtract(estimated);
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    String reason = "Over budget by " + categoryOverage + " across " + categoryItems.size() + " items";
                    String date = LocalDateTime.now().toString();
                    
                    return new BudgetContingencyResponse.ContingencyUsage(category, categoryOverage, reason, date);
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());
        
        return new BudgetContingencyResponse(
                budget.getContingencyAmount(),
                contingencyUsed,
                contingencyRemaining,
                budget.getContingencyPercentage(),
                contingencyStatus,
                usageBreakdown,
                generateContingencyRecommendations(contingencyStatus, contingencyRemaining)
        );
    }

    public CategoryBreakdownResponse getCategoryBreakdown(UUID budgetId) {
        List<BudgetLineItem> lineItems = listLineItems(budgetId);
        
        Map<String, BigDecimal> estimatedByCategory = getCategoryBreakdown(lineItems);
        Map<String, BigDecimal> actualByCategory = getCategoryActualBreakdown(lineItems);
        Map<String, BigDecimal> varianceByCategory = getCategoryVarianceBreakdown(lineItems);
        
        BigDecimal grandTotalEstimated = lineItems.stream()
                .map(item -> item.getEstimatedCost() != null ? item.getEstimatedCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<CategoryBreakdownResponse.CategorySummary> categorySummaries = lineItems.stream()
                .collect(Collectors.groupingBy(BudgetLineItem::getCategory))
                .entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<BudgetLineItem> items = entry.getValue();
                    
                    BigDecimal estimated = items.stream()
                            .map(item -> item.getEstimatedCost() != null ? item.getEstimatedCost() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    BigDecimal actual = items.stream()
                            .map(item -> item.getActualCost() != null ? item.getActualCost() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    BigDecimal variance = actual.subtract(estimated);
                    
                    BigDecimal percentageOfTotal = grandTotalEstimated.compareTo(BigDecimal.ZERO) > 0 ?
                            estimated.divide(grandTotalEstimated, 4, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100")) : BigDecimal.ZERO;
                    
                    String status = variance.compareTo(BigDecimal.ZERO) > 0 ? "OVER_BUDGET" :
                                  variance.compareTo(BigDecimal.ZERO) < 0 ? "UNDER_BUDGET" : "ON_BUDGET";
                    
                    return new CategoryBreakdownResponse.CategorySummary(category, estimated, actual, variance, percentageOfTotal, status);
                })
                .collect(Collectors.toList());
        
        BigDecimal totalEstimated = lineItems.stream()
                .map(item -> item.getEstimatedCost() != null ? item.getEstimatedCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalActual = lineItems.stream()
                .map(item -> item.getActualCost() != null ? item.getActualCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalVariance = totalActual.subtract(totalEstimated);
        
        return new CategoryBreakdownResponse(
                estimatedByCategory,
                actualByCategory,
                varianceByCategory,
                categorySummaries,
                totalEstimated,
                totalActual,
                totalVariance
        );
    }

    public void deleteBudget(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        UUID ownerId = budget.getOwnerId();
        UUID eventId = budget.getEventId();
        
        // Explicitly delete all line items first (cascade should handle this, but being explicit)
        List<BudgetLineItem> lineItems = lineItemRepository.findByBudgetId(budgetId);
        if (!lineItems.isEmpty()) {
            lineItemRepository.deleteAll(lineItems);
            
            // Audit log for line items deletion
            auditLogService.builder()
                .domain("BUDGET")
                .entityType("BudgetLineItem")
                .entityId(budgetId)
                .action(ActionType.BULK_DELETE)
                .user(ownerId, null, null)
                .description(String.format("Deleted %d line items associated with budget", lineItems.size()))
                .eventId(eventId)
                .log();
        }
        
        // Now delete the budget (soft delete due to @SQLDelete annotation)
        budgetRepository.delete(budget);
        
        // Audit log with unified audit system
        auditLogService.builder()
            .domain("BUDGET")
            .entityType("Budget")
            .entityId(budgetId)
            .action(ActionType.DELETE)
            .user(ownerId, null, null)
            .description(String.format("Budget deleted (soft delete) for event %s", eventId))
            .changes(budget, null)
            .eventId(eventId)
            .log();
    }

    public Budget recalculateTotals(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        recalculateBudgetTotals(budgetId);
        return budgetRepository.findById(budgetId).orElse(budget);
    }

    private void recalculateBudgetTotals(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId).orElse(null);
        if (budget == null) return;
        
        List<BudgetLineItem> lineItems = listLineItems(budgetId);
        
        BigDecimal totalEstimated = lineItems.stream()
                .map(item -> item.getEstimatedCost() != null ? item.getEstimatedCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalActual = lineItems.stream()
                .map(item -> item.getActualCost() != null ? item.getActualCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        budget.setTotalEstimated(totalEstimated);
        budget.setTotalActual(totalActual);
        budget.setVariance(totalActual.subtract(totalEstimated));
        
        if (totalEstimated.compareTo(BigDecimal.ZERO) > 0) {
            budget.setVariancePercentage(budget.getVariance().divide(totalEstimated, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")));
        }
        
        budgetRepository.save(budget);
    }

    private Map<String, BigDecimal> getCategoryBreakdown(List<BudgetLineItem> lineItems) {
        return lineItems.stream()
                .collect(Collectors.groupingBy(
                        BudgetLineItem::getCategory,
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> item.getEstimatedCost() != null ? item.getEstimatedCost() : BigDecimal.ZERO,
                                BigDecimal::add)
                ));
    }

    private Map<String, BigDecimal> getCategoryActualBreakdown(List<BudgetLineItem> lineItems) {
        return lineItems.stream()
                .collect(Collectors.groupingBy(
                        BudgetLineItem::getCategory,
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> item.getActualCost() != null ? item.getActualCost() : BigDecimal.ZERO,
                                BigDecimal::add)
                ));
    }

    private Map<String, BigDecimal> getCategoryVarianceBreakdown(List<BudgetLineItem> lineItems) {
        return lineItems.stream()
                .collect(Collectors.groupingBy(
                        BudgetLineItem::getCategory,
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> {
                                    BigDecimal actual = item.getActualCost() != null ? item.getActualCost() : BigDecimal.ZERO;
                                    BigDecimal estimated = item.getEstimatedCost() != null ? item.getEstimatedCost() : BigDecimal.ZERO;
                                    return actual.subtract(estimated);
                                },
                                BigDecimal::add)
                ));
    }

    private String determineRiskLevel(BigDecimal spentPercentage, BigDecimal variancePercentage) {
        if (spentPercentage.compareTo(new BigDecimal("90")) > 0 || variancePercentage.compareTo(new BigDecimal("20")) > 0) {
            return "CRITICAL";
        } else if (spentPercentage.compareTo(new BigDecimal("75")) > 0 || variancePercentage.compareTo(new BigDecimal("10")) > 0) {
            return "HIGH";
        } else if (spentPercentage.compareTo(new BigDecimal("50")) > 0 || variancePercentage.compareTo(new BigDecimal("5")) > 0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String determineContingencyStatus(BigDecimal contingencyRemaining, BigDecimal totalContingency) {
        if (contingencyRemaining.compareTo(BigDecimal.ZERO) < 0) {
            return "CRITICAL";
        } else if (totalContingency != null && contingencyRemaining.divide(totalContingency, 4, RoundingMode.HALF_UP).compareTo(new BigDecimal("0.2")) < 0) {
            return "WARNING";
        } else {
            return "SAFE";
        }
    }

    private String generateRecommendations(BigDecimal variance, BigDecimal spentPercentage, String riskLevel) {
        List<String> recommendations = new ArrayList<>();
        
        if (variance.compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add("Consider reducing costs in over-budget categories");
        }
        
        if (spentPercentage.compareTo(new BigDecimal("80")) > 0) {
            recommendations.add("Monitor spending closely - approaching budget limit");
        }
        
        if ("CRITICAL".equals(riskLevel)) {
            recommendations.add("Immediate action required - budget at risk");
        }
        
        return String.join("; ", recommendations);
    }

    private String generateVarianceAnalysisSummary(BigDecimal totalVariance, List<BudgetVarianceAnalysisResponse.CategoryVariance> categoryVariances) {
        if (totalVariance.compareTo(BigDecimal.ZERO) > 0) {
            return "Budget is over by " + totalVariance + " across " + categoryVariances.size() + " categories";
        } else if (totalVariance.compareTo(BigDecimal.ZERO) < 0) {
            return "Budget is under by " + totalVariance.abs() + " - good cost control";
        } else {
            return "Budget is exactly on target";
        }
    }

    private String generateContingencyRecommendations(String contingencyStatus, BigDecimal contingencyRemaining) {
        switch (contingencyStatus) {
            case "CRITICAL":
                return "Contingency exhausted - immediate budget review required";
            case "WARNING":
                return "Contingency running low - monitor spending carefully";
            default:
                return "Contingency levels are healthy";
        }
    }

    // ==================== VALIDATION METHODS ====================

    private void validateBudget(Budget budget) {
        if (budget.getTotalBudget() != null) {
            validatePositiveAmount(budget.getTotalBudget(), "Total budget");
        }
        
        if (budget.getContingencyPercentage() != null) {
            validatePercentage(budget.getContingencyPercentage(), "Contingency percentage");
        }
        
        if (budget.getCurrency() != null) {
            validateCurrency(budget.getCurrency());
        }
        
        if (budget.getEventId() == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
        
        if (budget.getOwnerId() == null) {
            throw new IllegalArgumentException("Owner ID is required");
        }
    }

    private void validatePositiveAmount(BigDecimal amount, String fieldName) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }

    private void validatePercentage(BigDecimal percentage, String fieldName) {
        if (percentage != null) {
            if (percentage.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(fieldName + " cannot be negative");
            }
            if (percentage.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException(fieldName + " cannot exceed 100%");
            }
        }
    }

    private void validateCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be empty");
        }
        if (currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code (e.g., USD, EUR, GBP)");
        }
        // List of common ISO 4217 currency codes
        List<String> validCurrencies = List.of(
            "USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "NZD", 
            "CNY", "INR", "BRL", "ZAR", "MXN", "SGD", "HKD", "SEK", 
            "NOK", "DKK", "PLN", "THB", "IDR", "MYR", "PHP", "CZK", 
            "HUF", "ILS", "CLP", "ARS", "COP", "PEN", "RUB", "TRY",
            "KRW", "TWD", "SAR", "AED", "QAR", "KWD", "BHD", "OMR",
            "EGP", "NGN", "KES", "GHS", "MAD", "TND", "XOF", "XAF"
        );
        if (!validCurrencies.contains(currency.toUpperCase())) {
            throw new IllegalArgumentException("Invalid currency code: " + currency + ". Must be a valid ISO 4217 code.");
        }
    }
}


