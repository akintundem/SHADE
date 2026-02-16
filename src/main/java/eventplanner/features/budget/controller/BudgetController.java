package eventplanner.features.budget.controller;

import eventplanner.features.budget.dto.request.BudgetLineItemAutoSaveRequest;
import eventplanner.features.budget.dto.request.UpdateBudgetRequest;
import eventplanner.features.budget.dto.response.BudgetCategoryResponse;
import eventplanner.features.budget.dto.response.BudgetDetailResponse;
import eventplanner.features.budget.dto.response.BudgetLineItemResponse;
import eventplanner.features.budget.entity.Budget;
import eventplanner.features.budget.entity.BudgetCategory;
import eventplanner.features.budget.entity.BudgetLineItem;
import eventplanner.features.budget.service.BudgetService;
import eventplanner.features.budget.util.BudgetHttpUtil;
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
import eventplanner.security.authorization.rbac.constants.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import eventplanner.security.auth.service.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Budget Controller with authorization checks
 */
@RestController
@RequestMapping("/api/v1/events/{eventId}/budget")
@Tag(name = "Budget Management", description = "Comprehensive budget management operations")
public class BudgetController {

	private final BudgetService budgetService;

	public BudgetController(BudgetService budgetService) {
		this.budgetService = budgetService;
	}

	// ==================== CORE BUDGET CRUD ====================
	// Authorization is handled solely by @RequiresPermission (RBAC) to avoid divergence
	// between dual authorization layers.

	@GetMapping
    @RequiresPermission(value = RbacPermissions.BUDGET_READ, resources = {"event_id=#eventId"})
	@Operation(summary = "Get event budget", description = "Retrieve budget for a specific event")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget found", 
			content = @Content(schema = @Schema(implementation = BudgetDetailResponse.class))),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "400", description = "Invalid event ID format"),
		@ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated")
	})
	public ResponseEntity<BudgetDetailResponse> getBudget(
			@PathVariable String eventId,
			@AuthenticationPrincipal UserPrincipal user) {
		UUID uuid = UUID.fromString(eventId);
		Budget budget = budgetService.getByEventId(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
		return BudgetHttpUtil.wrapResponse(budget, BudgetDetailResponse.fromEntity(budget));
	}

	@PutMapping
    @RequiresPermission(value = RbacPermissions.BUDGET_UPDATE, resources = {"event_id=#eventId"})
	@Operation(summary = "Update budget details", description = "Update budget information including total budget and notes")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget updated successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request data"),
		@ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated")
	})
	public ResponseEntity<BudgetDetailResponse> updateBudget(
			@PathVariable String eventId,
			@Valid @RequestBody UpdateBudgetRequest request,
			@RequestHeader(value = "If-Match", required = false) String ifMatch,
			@AuthenticationPrincipal UserPrincipal user) {
		UUID uuid = UUID.fromString(eventId);
		Budget existing = budgetService.getByEventId(uuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));

		// Validate ETag if provided
		if (ifMatch != null && !ifMatch.equals(BudgetHttpUtil.generateETag(existing))) {
			throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Budget has been modified.");
		}

		Budget updated = budgetService.updateBudget(existing.getId(), request);
		return BudgetHttpUtil.wrapResponse(updated, BudgetDetailResponse.fromEntity(updated));
	}

	// ==================== CATEGORY MANAGEMENT ====================

	@GetMapping("/categories")
    @RequiresPermission(value = RbacPermissions.BUDGET_READ, resources = {"event_id=#eventId"})
	@Operation(summary = "Get all categories", description = "Retrieve all categories for the event budget")
	public ResponseEntity<List<BudgetCategoryResponse>> getCategories(
			@PathVariable String eventId,
			@AuthenticationPrincipal UserPrincipal user) {
		UUID eventUuid = UUID.fromString(eventId);
		Budget budget = budgetService.getByEventId(eventUuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
		List<BudgetCategory> categories = budgetService.listCategories(budget.getId());
		return ResponseEntity.ok(categories.stream().map(BudgetCategoryResponse::fromEntity).toList());
	}

	// ==================== LINE ITEM MANAGEMENT (EXPENSES) ====================

	@PatchMapping("/line-items/auto-save")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_UPDATE, resources = {"event_id=#eventId"})
	@Operation(summary = "Auto-save expense draft", description = "Create or update a line item as draft. If id is null, creates new. If id is provided, updates existing.")
	public ResponseEntity<BudgetLineItemResponse> autoSaveDraft(
			@PathVariable String eventId,
			@Valid @RequestBody BudgetLineItemAutoSaveRequest request) {
		UUID eventUuid = UUID.fromString(eventId);
		Budget budget = budgetService.getByEventId(eventUuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
		BudgetLineItem saved = budgetService.autoSaveLineItemDraft(budget, request);
		return ResponseEntity.ok(BudgetLineItemResponse.fromEntity(saved));
	}

	@GetMapping("/line-items")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_READ, resources = {"event_id=#eventId"})
	@Operation(summary = "Get all expenses", description = "Retrieve all line items for the event budget")
	public ResponseEntity<List<BudgetLineItemResponse>> getLineItems(
			@PathVariable String eventId) {
		UUID eventUuid = UUID.fromString(eventId);
		Budget budget = budgetService.getByEventId(eventUuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
		List<BudgetLineItem> items = budgetService.listLineItems(budget.getId());
		return ResponseEntity.ok(items.stream().map(BudgetLineItemResponse::fromEntity).toList());
	}

	@DeleteMapping("/line-items/{itemId}")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_DELETE, resources = {"event_id=#eventId"})
	@Operation(summary = "Delete expense", description = "Remove a line item")
	public ResponseEntity<Void> deleteLineItem(
			@PathVariable String eventId,
			@PathVariable String itemId) {
		UUID itemUuid = UUID.fromString(itemId);
		UUID eventUuid = UUID.fromString(eventId);
		Budget budget = budgetService.getByEventId(eventUuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
		budgetService.deleteLineItem(budget, itemUuid);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/line-items/{itemId}/finalize")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_UPDATE, resources = {"event_id=#eventId"})
	@Operation(summary = "Finalize expense", description = "Publish a draft line item. Deletes the item if it has no content.")
	public ResponseEntity<BudgetLineItemResponse> finalizeLineItem(
			@PathVariable String eventId,
			@PathVariable String itemId,
			@Valid @RequestBody BudgetLineItemAutoSaveRequest request) {
		UUID itemUuid = UUID.fromString(itemId);
		UUID eventUuid = UUID.fromString(eventId);
		Budget budget = budgetService.getByEventId(eventUuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
		return budgetService.finalizeLineItem(budget, itemUuid, request)
				.map(finalized -> ResponseEntity.ok(BudgetLineItemResponse.fromEntity(finalized)))
				.orElseGet(() -> ResponseEntity.noContent().build());
	}
}
