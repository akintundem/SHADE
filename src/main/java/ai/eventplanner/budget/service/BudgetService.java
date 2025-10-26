package ai.eventplanner.budget.service;

import ai.eventplanner.budget.dto.request.UpdateBudgetRequest;
import ai.eventplanner.budget.dto.request.UpdateBudgetLineItemRequest;
import ai.eventplanner.budget.dto.request.BudgetApprovalRequest;
import ai.eventplanner.budget.dto.request.BulkLineItemRequest;
import ai.eventplanner.budget.dto.response.*;
import ai.eventplanner.budget.model.BudgetEntity;
import ai.eventplanner.budget.model.BudgetLineItemEntity;
import ai.eventplanner.budget.repo.BudgetLineItemRepository;
import ai.eventplanner.budget.repo.BudgetRepository;
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

    public BudgetService(BudgetRepository budgetRepository, BudgetLineItemRepository lineItemRepository) {
        this.budgetRepository = budgetRepository;
        this.lineItemRepository = lineItemRepository;
    }

    public Optional<BudgetEntity> getByEventId(UUID eventId) {
        return budgetRepository.findByEventId(eventId);
    }

    public Optional<BudgetEntity> getById(UUID budgetId) {
        return budgetRepository.findById(budgetId);
    }

    public BudgetEntity createOrUpdate(BudgetEntity budget) {
        if (budget.getContingencyPercentage() != null && budget.getTotalBudget() != null) {
            budget.setContingencyAmount(budget.getTotalBudget().multiply(budget.getContingencyPercentage()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        }
        return budgetRepository.save(budget);
    }

    public BudgetEntity updateBudget(UUID budgetId, UpdateBudgetRequest request) {
        BudgetEntity budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        if (request.getTotalBudget() != null) {
            budget.setTotalBudget(request.getTotalBudget());
        }
        if (request.getContingencyPercentage() != null) {
            budget.setContingencyPercentage(request.getContingencyPercentage());
        }
        if (request.getCurrency() != null) {
            budget.setCurrency(request.getCurrency());
        }
        if (request.getNotes() != null) {
            budget.setNotes(request.getNotes());
        }
        
        // Recalculate contingency amount
        if (budget.getContingencyPercentage() != null && budget.getTotalBudget() != null) {
            budget.setContingencyAmount(budget.getTotalBudget().multiply(budget.getContingencyPercentage()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
        }
        
        return budgetRepository.save(budget);
    }

    public BudgetLineItemEntity addLineItem(BudgetLineItemEntity item) {
        BudgetLineItemEntity saved = lineItemRepository.save(item);
        recalculateBudgetTotals(item.getBudgetId());
        return saved;
    }

    public BudgetLineItemEntity updateLineItem(UUID itemId, UpdateBudgetLineItemRequest request) {
        BudgetLineItemEntity item = lineItemRepository.findById(itemId)
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
        
        BudgetLineItemEntity saved = lineItemRepository.save(item);
        recalculateBudgetTotals(item.getBudgetId());
        return saved;
    }

    public void deleteLineItem(UUID itemId) {
        BudgetLineItemEntity item = lineItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Line item not found"));
        UUID budgetId = item.getBudgetId();
        lineItemRepository.deleteById(itemId);
        recalculateBudgetTotals(budgetId);
    }

    public List<BudgetLineItemEntity> addBulkLineItems(BulkLineItemRequest request) {
        List<BudgetLineItemEntity> items = request.getLineItems().stream()
                .map(req -> {
                    BudgetLineItemEntity item = new BudgetLineItemEntity();
                    item.setBudgetId(request.getBudgetId());
                    item.setCategory(req.getCategory());
                    item.setDescription(req.getDescription());
                    item.setEstimatedCost(req.getEstimatedCost());
                    item.setActualCost(req.getActualCost());
                    item.setVendorId(req.getVendorId());
                    return item;
                })
                .collect(Collectors.toList());
        
        List<BudgetLineItemEntity> saved = lineItemRepository.saveAll(items);
        recalculateBudgetTotals(request.getBudgetId());
        return saved;
    }

    public List<BudgetLineItemEntity> listLineItems(UUID budgetId) {
        return lineItemRepository.findByBudgetId(budgetId);
    }

    public Optional<BudgetLineItemEntity> getLineItem(UUID itemId) {
        return lineItemRepository.findById(itemId);
    }

    public BigDecimal computeRollup(UUID budgetId) {
        return listLineItems(budgetId).stream()
                .map(it -> it.getActualCost() != null ? it.getActualCost() : (it.getEstimatedCost() != null ? it.getEstimatedCost() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BudgetEntity approveBudget(UUID budgetId, BudgetApprovalRequest request) {
        BudgetEntity budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        if (!"DRAFT".equals(budget.getBudgetStatus()) && !"SUBMITTED".equals(budget.getBudgetStatus())) {
            throw new RuntimeException("Budget cannot be approved in current status: " + budget.getBudgetStatus());
        }
        
        budget.setBudgetStatus("APPROVED");
        budget.setApprovedBy(request.getApprovedBy());
        budget.setApprovedDate(LocalDateTime.now());
        if (request.getNotes() != null) {
            budget.setNotes(request.getNotes());
        }
        
        return budgetRepository.save(budget);
    }

    public BudgetEntity rejectBudget(UUID budgetId, BudgetApprovalRequest request) {
        BudgetEntity budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        if (!"SUBMITTED".equals(budget.getBudgetStatus())) {
            throw new RuntimeException("Budget cannot be rejected in current status: " + budget.getBudgetStatus());
        }
        
        budget.setBudgetStatus("REJECTED");
        budget.setApprovedBy(request.getApprovedBy());
        budget.setApprovedDate(LocalDateTime.now());
        if (request.getNotes() != null) {
            budget.setNotes(request.getNotes());
        }
        
        return budgetRepository.save(budget);
    }

    public BudgetEntity submitForApproval(UUID budgetId) {
        BudgetEntity budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        if (!"DRAFT".equals(budget.getBudgetStatus())) {
            throw new RuntimeException("Budget cannot be submitted in current status: " + budget.getBudgetStatus());
        }
        
        budget.setBudgetStatus("SUBMITTED");
        return budgetRepository.save(budget);
    }

    public BudgetSummaryResponse getBudgetSummary(UUID budgetId) {
        BudgetEntity budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        List<BudgetLineItemEntity> lineItems = listLineItems(budgetId);
        
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
        List<BudgetLineItemEntity> lineItems = listLineItems(budgetId);
        
        List<BudgetVarianceAnalysisResponse.CategoryVariance> categoryVariances = lineItems.stream()
                .collect(Collectors.groupingBy(BudgetLineItemEntity::getCategory))
                .entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<BudgetLineItemEntity> items = entry.getValue();
                    
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
        BudgetEntity budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        List<BudgetLineItemEntity> lineItems = listLineItems(budgetId);
        BigDecimal totalActual = lineItems.stream()
                .map(item -> item.getActualCost() != null ? item.getActualCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal contingencyUsed = budget.getTotalBudget().subtract(totalActual);
        BigDecimal contingencyRemaining = budget.getContingencyAmount() != null ? 
                budget.getContingencyAmount().subtract(contingencyUsed) : BigDecimal.ZERO;
        
        String contingencyStatus = determineContingencyStatus(contingencyRemaining, budget.getContingencyAmount());
        
        return new BudgetContingencyResponse(
                budget.getContingencyAmount(),
                contingencyUsed,
                contingencyRemaining,
                budget.getContingencyPercentage(),
                contingencyStatus,
                new ArrayList<>(), // TODO: Implement usage breakdown
                generateContingencyRecommendations(contingencyStatus, contingencyRemaining)
        );
    }

    public CategoryBreakdownResponse getCategoryBreakdown(UUID budgetId) {
        List<BudgetLineItemEntity> lineItems = listLineItems(budgetId);
        return new CategoryBreakdownResponse(
                getCategoryBreakdown(lineItems),
                getCategoryBreakdown(lineItems),
                getCategoryBreakdown(lineItems),
                new ArrayList<>(), // TODO: Implement category summaries
                BigDecimal.ZERO, // TODO: Calculate totals
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    public void deleteBudget(UUID budgetId) {
        budgetRepository.deleteById(budgetId);
    }

    public BudgetEntity recalculateTotals(UUID budgetId) {
        BudgetEntity budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Budget not found"));
        
        recalculateBudgetTotals(budgetId);
        return budgetRepository.findById(budgetId).orElse(budget);
    }

    private void recalculateBudgetTotals(UUID budgetId) {
        BudgetEntity budget = budgetRepository.findById(budgetId).orElse(null);
        if (budget == null) return;
        
        List<BudgetLineItemEntity> lineItems = listLineItems(budgetId);
        
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

    private Map<String, BigDecimal> getCategoryBreakdown(List<BudgetLineItemEntity> lineItems) {
        return lineItems.stream()
                .collect(Collectors.groupingBy(
                        BudgetLineItemEntity::getCategory,
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> item.getEstimatedCost() != null ? item.getEstimatedCost() : BigDecimal.ZERO,
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
}


