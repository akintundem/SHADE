package eventplanner.features.budget.controller;

import eventplanner.features.budget.dto.BudgetLineItemCreateRequest;
import eventplanner.features.budget.dto.BudgetUpsertRequest;
import eventplanner.features.budget.dto.request.*;
import eventplanner.features.budget.dto.response.*;
import eventplanner.features.budget.entity.Budget;
import eventplanner.features.budget.entity.BudgetLineItem;
import eventplanner.features.budget.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;

/**
 * Budget Controller with authorization checks
 */
@RestController
@RequestMapping("/api/v1/budgets")
@Tag(name = "Budget Management", description = "Comprehensive budget management operations")
public class BudgetController {

	private final BudgetService budgetService;

	public BudgetController(BudgetService budgetService) {
		this.budgetService = budgetService;
	}

	// ==================== CORE BUDGET CRUD ====================

	@GetMapping("/{eventId}")
    @RequiresPermission(value = RbacPermissions.BUDGET_READ, resources = {"event_id=#eventId"})
	@Operation(summary = "Get event budget", description = "Retrieve budget for a specific event")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget found", 
			content = @Content(schema = @Schema(implementation = BudgetDetailResponse.class))),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "400", description = "Invalid event ID format")
	})
	public ResponseEntity<BudgetDetailResponse> getBudget(@PathVariable String eventId) {
		try {
			UUID uuid = UUID.fromString(eventId);
			return budgetService.getByEventId(uuid)
				.map(this::convertToDetailResponse)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event ID format", e);
		}
	}

	@PostMapping
    @RequiresPermission(value = RbacPermissions.BUDGET_CREATE, resources = {"event_id=#budget.eventId"})
	@Operation(summary = "Create or update budget", description = "Create a new budget or update existing one")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget created/updated successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	public ResponseEntity<BudgetDetailResponse> upsert(@Valid @RequestBody BudgetUpsertRequest budget) {
		if (budget == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget data is required");
		}
		
		Budget entity = new Budget();
		entity.setEventId(budget.getEventId());
		entity.setTotalBudget(budget.getTotalBudget());
		entity.setCurrency(budget.getCurrency());
		
		Budget saved = budgetService.createOrUpdate(entity);
		return ResponseEntity.ok(convertToDetailResponse(saved));
	}

	@PutMapping("/{budgetId}")
    @RequiresPermission(value = RbacPermissions.BUDGET_UPDATE, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Update budget details", description = "Update budget information including contingency settings")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget updated successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	public ResponseEntity<BudgetDetailResponse> updateBudget(
			@PathVariable String budgetId,
			@Valid @RequestBody UpdateBudgetRequest request) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			Budget updated = budgetService.updateBudget(uuid, request);
			return ResponseEntity.ok(convertToDetailResponse(updated));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (RuntimeException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@DeleteMapping("/{budgetId}")
    @RequiresPermission(value = RbacPermissions.BUDGET_DELETE, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Delete budget", description = "Permanently delete a budget and all its line items")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "Budget deleted successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<Void> deleteBudget(@PathVariable String budgetId) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			budgetService.deleteBudget(uuid);
			return ResponseEntity.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		}
	}

	// ==================== LINE ITEM MANAGEMENT ====================

	@PostMapping("/{budgetId}/line-items")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_CREATE, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Add budget line item", description = "Add a single line item to the budget")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Line item added successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	public ResponseEntity<BudgetLineItemResponse> addLineItem(
			@PathVariable String budgetId,
			@Valid @RequestBody BudgetLineItemCreateRequest payload) {
		try {
			UUID budgetUuid = UUID.fromString(budgetId);
            
            BudgetLineItem item = new BudgetLineItem();
			item.setBudgetId(budgetUuid);
            item.setCategory(payload.getCategory());
            item.setDescription(payload.getDescription());
            item.setEstimatedCost(payload.getEstimatedCost());
            item.setActualCost(payload.getActualCost());
            item.setVendorId(payload.getVendorId());
			
			BudgetLineItem saved = budgetService.addLineItem(item);
			return ResponseEntity.ok(convertToLineItemResponse(saved));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		}
	}

	@PostMapping("/{budgetId}/line-items/bulk")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_CREATE, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Add multiple line items", description = "Add multiple line items to the budget in one operation")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Line items added successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	public ResponseEntity<List<BudgetLineItemResponse>> addBulkLineItems(
			@PathVariable String budgetId,
			@Valid @RequestBody BulkLineItemRequest request) {
		try {
			UUID budgetUuid = UUID.fromString(budgetId);
			request.setBudgetId(budgetUuid);
			
			List<BudgetLineItem> saved = budgetService.addBulkLineItems(request);
			List<BudgetLineItemResponse> responses = saved.stream()
					.map(this::convertToLineItemResponse)
					.toList();
			return ResponseEntity.ok(responses);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		}
	}

	@GetMapping("/{budgetId}/line-items")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_READ, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Get all line items", description = "Retrieve all line items for a budget")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Line items retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<List<BudgetLineItemResponse>> getLineItems(@PathVariable String budgetId) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			List<BudgetLineItem> items = budgetService.listLineItems(uuid);
			List<BudgetLineItemResponse> responses = items.stream()
					.map(this::convertToLineItemResponse)
					.toList();
			return ResponseEntity.ok(responses);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		}
	}

	@GetMapping("/{budgetId}/line-items/{itemId}")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_READ, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Get specific line item", description = "Retrieve a specific line item by ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Line item found"),
		@ApiResponse(responseCode = "404", description = "Line item not found")
	})
	public ResponseEntity<BudgetLineItemResponse> getLineItem(
			@PathVariable String budgetId,
			@PathVariable String itemId) {
		try {
			UUID itemUuid = UUID.fromString(itemId);
			return budgetService.getLineItem(itemUuid)
					.map(this::convertToLineItemResponse)
					.map(ResponseEntity::ok)
					.orElseGet(() -> ResponseEntity.notFound().build());
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ID format", e);
		}
	}

	@PutMapping("/{budgetId}/line-items/{itemId}")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_UPDATE, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Update line item", description = "Update an existing line item")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Line item updated successfully"),
		@ApiResponse(responseCode = "404", description = "Line item not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	public ResponseEntity<BudgetLineItemResponse> updateLineItem(
			@PathVariable String budgetId,
			@PathVariable String itemId,
			@Valid @RequestBody UpdateBudgetLineItemRequest request) {
		try {
			UUID itemUuid = UUID.fromString(itemId);
			BudgetLineItem updated = budgetService.updateLineItem(itemUuid, request);
			return ResponseEntity.ok(convertToLineItemResponse(updated));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ID format", e);
		} catch (RuntimeException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@DeleteMapping("/{budgetId}/line-items/{itemId}")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_DELETE, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Delete line item", description = "Remove a line item from the budget")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "Line item deleted successfully"),
		@ApiResponse(responseCode = "404", description = "Line item not found")
	})
	public ResponseEntity<Void> deleteLineItem(
			@PathVariable String budgetId,
			@PathVariable String itemId) {
		try {
			UUID itemUuid = UUID.fromString(itemId);
			budgetService.deleteLineItem(itemUuid);
			return ResponseEntity.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ID format", e);
		} catch (RuntimeException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	// ==================== ANALYSIS ENDPOINTS ====================

	@GetMapping("/{budgetId}/summary")
    @RequiresPermission(value = RbacPermissions.BUDGET_READ, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Get budget summary", description = "Get comprehensive budget summary with key metrics")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<BudgetSummaryResponse> getBudgetSummary(@PathVariable String budgetId) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			BudgetSummaryResponse summary = budgetService.getBudgetSummary(uuid);
			return ResponseEntity.ok(summary);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (RuntimeException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@GetMapping("/{budgetId}/variance-analysis")
    @RequiresPermission(value = RbacPermissions.BUDGET_ANALYTICS_READ, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Get variance analysis", description = "Get detailed variance analysis by category")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Analysis retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<BudgetVarianceAnalysisResponse> getVarianceAnalysis(@PathVariable String budgetId) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			BudgetVarianceAnalysisResponse analysis = budgetService.getVarianceAnalysis(uuid);
			return ResponseEntity.ok(analysis);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (RuntimeException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@GetMapping("/{budgetId}/contingency-analysis")
    @RequiresPermission(value = RbacPermissions.BUDGET_ANALYTICS_READ, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Get contingency analysis", description = "Get contingency usage and recommendations")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Analysis retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<BudgetContingencyResponse> getContingencyAnalysis(@PathVariable String budgetId) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			BudgetContingencyResponse analysis = budgetService.getContingencyAnalysis(uuid);
			return ResponseEntity.ok(analysis);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (RuntimeException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@GetMapping("/{budgetId}/category-breakdown")
    @RequiresPermission(value = RbacPermissions.BUDGET_ANALYTICS_READ, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Get category breakdown", description = "Get spending breakdown by category")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Breakdown retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<CategoryBreakdownResponse> getCategoryBreakdown(@PathVariable String budgetId) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			CategoryBreakdownResponse breakdown = budgetService.getCategoryBreakdown(uuid);
			return ResponseEntity.ok(breakdown);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (RuntimeException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	// ==================== APPROVAL WORKFLOW ====================

	@PostMapping("/{budgetId}/submit-for-approval")
    @RequiresPermission(value = RbacPermissions.BUDGET_SUBMIT, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Submit budget for approval", description = "Submit budget for approval workflow")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget submitted for approval"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "409", description = "Budget cannot be submitted in current status")
	})
	public ResponseEntity<BudgetDetailResponse> submitForApproval(@PathVariable String budgetId) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			Budget updated = budgetService.submitForApproval(uuid);
			return ResponseEntity.ok(convertToDetailResponse(updated));
        } catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (RuntimeException e) {
			if (e.getMessage().contains("not found")) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
			} else {
				throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
			}
		}
	}

	@PostMapping("/{budgetId}/approve")
    @RequiresPermission(value = RbacPermissions.BUDGET_APPROVE, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Approve budget", description = "Approve the budget")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget approved successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "409", description = "Budget cannot be approved in current status")
	})
	public ResponseEntity<BudgetDetailResponse> approveBudget(
			@PathVariable String budgetId,
			@Valid @RequestBody BudgetApprovalRequest request) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			Budget updated = budgetService.approveBudget(uuid, request);
			return ResponseEntity.ok(convertToDetailResponse(updated));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (RuntimeException e) {
			if (e.getMessage().contains("not found")) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
			} else {
				throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
			}
		}
	}

	@PostMapping("/{budgetId}/reject")
    @RequiresPermission(value = RbacPermissions.BUDGET_REJECT, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Reject budget", description = "Reject the budget with reason")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget rejected successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "409", description = "Budget cannot be rejected in current status")
	})
	public ResponseEntity<BudgetDetailResponse> rejectBudget(
			@PathVariable String budgetId,
			@Valid @RequestBody BudgetApprovalRequest request) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			Budget updated = budgetService.rejectBudget(uuid, request);
			return ResponseEntity.ok(convertToDetailResponse(updated));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (RuntimeException e) {
			if (e.getMessage().contains("not found")) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
			} else {
				throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
			}
		}
	}

	// ==================== UTILITY ENDPOINTS ====================

	@GetMapping("/{budgetId}/rollup")
    @RequiresPermission(value = RbacPermissions.BUDGET_ANALYTICS_READ, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Compute rollup total", description = "Compute total rollup for a budget")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Rollup computed successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid budget ID format")
	})
	public ResponseEntity<java.math.BigDecimal> rollup(@PathVariable String budgetId) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			return ResponseEntity.ok(budgetService.computeRollup(uuid));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		}
	}

	@PostMapping("/{budgetId}/recalculate")
    @RequiresPermission(value = RbacPermissions.BUDGET_RECALCULATE, resources = {"budget_id=#budgetId"})
	@Operation(summary = "Recalculate totals", description = "Force recalculation of budget totals and variance")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Totals recalculated successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<BudgetDetailResponse> recalculateTotals(@PathVariable String budgetId) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			Budget updated = budgetService.recalculateTotals(uuid);
			return ResponseEntity.ok(convertToDetailResponse(updated));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (RuntimeException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
		}
	}

	@GetMapping("/categories")
    @RequiresPermission(RbacPermissions.BUDGET_READ)
	@Operation(summary = "Get standard budget categories", description = "Get list of standard budget categories")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
	})
	public ResponseEntity<List<String>> getStandardCategories() {
		List<String> categories = List.of(
			"Venue & Facilities",
			"Catering & Food",
			"Entertainment & Activities",
			"Decorations & Flowers",
			"Transportation",
			"Staff & Labor",
			"Marketing & Promotion",
			"Insurance & Permits",
			"Technical Equipment",
			"Security",
			"Miscellaneous"
		);
		return ResponseEntity.ok(categories);
	}

	// ==================== HELPER METHODS ====================

	private BudgetDetailResponse convertToDetailResponse(Budget entity) {
		BudgetDetailResponse response = new BudgetDetailResponse();
		response.setId(entity.getId());
		response.setEventId(entity.getEventId());
		response.setTotalBudget(entity.getTotalBudget());
		response.setCurrency(entity.getCurrency());
		response.setContingencyPercentage(entity.getContingencyPercentage());
		response.setContingencyAmount(entity.getContingencyAmount());
		response.setTotalEstimated(entity.getTotalEstimated());
		response.setTotalActual(entity.getTotalActual());
		response.setVariance(entity.getVariance());
		response.setVariancePercentage(entity.getVariancePercentage());
		response.setBudgetStatus(entity.getBudgetStatus());
		response.setApprovedBy(entity.getApprovedBy());
		response.setApprovedDate(entity.getApprovedDate());
		response.setNotes(entity.getNotes());
		response.setCreatedAt(entity.getCreatedAt());
		response.setUpdatedAt(entity.getUpdatedAt());
		
		// Convert line items
		if (entity.getLineItems() != null) {
			List<BudgetLineItemResponse> lineItemResponses = entity.getLineItems().stream()
					.map(this::convertToLineItemResponse)
					.toList();
			response.setLineItems(lineItemResponses);
		}
		
		return response;
	}

	private BudgetLineItemResponse convertToLineItemResponse(BudgetLineItem entity) {
		BudgetLineItemResponse response = new BudgetLineItemResponse();
		response.setId(entity.getId());
		response.setBudgetId(entity.getBudgetId());
		response.setCategory(entity.getCategory());
		response.setSubcategory(entity.getSubcategory());
		response.setDescription(entity.getDescription());
		response.setEstimatedCost(entity.getEstimatedCost());
		response.setActualCost(entity.getActualCost());
		response.setVariance(entity.getVariance());
		response.setVariancePercentage(entity.getVariancePercentage());
		response.setQuantity(entity.getQuantity());
		response.setUnitCost(entity.getUnitCost());
		response.setPlanningStatus(entity.getPlanningStatus());
		response.setIsEssential(entity.getIsEssential());
		response.setPriority(entity.getPriority());
		response.setNotes(entity.getNotes());
		response.setCreatedAt(entity.getCreatedAt());
		response.setUpdatedAt(entity.getUpdatedAt());
		return response;
	}
}
