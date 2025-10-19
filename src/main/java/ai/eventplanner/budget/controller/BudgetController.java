package ai.eventplanner.budget.controller;

import ai.eventplanner.budget.dto.BudgetLineItemCreateRequest;
import ai.eventplanner.budget.dto.BudgetUpsertRequest;
import ai.eventplanner.budget.model.BudgetEntity;
import ai.eventplanner.budget.model.BudgetLineItemEntity;
import ai.eventplanner.budget.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/budgets")
@Tag(name = "Budgets")
public class BudgetController {

	private final BudgetService budgetService;

	public BudgetController(BudgetService budgetService) {
		this.budgetService = budgetService;
	}

	@GetMapping("/{eventId}")
	@Operation(summary = "Get event budget")
	public ResponseEntity<BudgetEntity> getBudget(@PathVariable String eventId) {
		return budgetService.getByEventId(java.util.UUID.fromString(eventId))
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

    @PostMapping("/{eventId}/line-items")
    @Operation(summary = "Add budget line item")
    public ResponseEntity<BudgetLineItemEntity> addLineItem(@PathVariable String eventId, @Valid @RequestBody BudgetLineItemCreateRequest payload) {
        BudgetLineItemEntity item = new BudgetLineItemEntity();
        item.setBudgetId(payload.getBudgetId());
        item.setCategory(payload.getCategory());
        item.setDescription(payload.getDescription());
        item.setEstimatedCost(payload.getEstimatedCost());
        item.setActualCost(payload.getActualCost());
        item.setVendorId(payload.getVendorId());
        return ResponseEntity.ok(budgetService.addLineItem(item));
    }

    @PostMapping
    @Operation(summary = "Create or update budget")
    public ResponseEntity<BudgetEntity> upsert(@Valid @RequestBody BudgetUpsertRequest budget) {
        BudgetEntity entity = new BudgetEntity();
        entity.setEventId(budget.getEventId());
        entity.setTotalBudget(budget.getTotalBudget());
        entity.setCurrency(budget.getCurrency());
        return ResponseEntity.ok(budgetService.createOrUpdate(entity));
    }

	@GetMapping("/{budgetId}/rollup")
	@Operation(summary = "Compute rollup total for a budget")
	public ResponseEntity<java.math.BigDecimal> rollup(@PathVariable String budgetId) {
		return ResponseEntity.ok(budgetService.computeRollup(java.util.UUID.fromString(budgetId)));
	}
}

