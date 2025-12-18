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
import eventplanner.security.authorization.rbac.RbacPermissions;
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
		if (user == null || user.getUser() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
		}
		
		try {
			UUID uuid = UUID.fromString(eventId);
			// Use getOrCreateByEventId to ensure budget exists
			Budget budget = budgetService.getOrCreateByEventId(uuid);
			return BudgetHttpUtil.wrapResponse(budget, BudgetDetailResponse.fromEntity(budget));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event ID format", e);
		}
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
		if (user == null || user.getUser() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
		}
		
		try {
			UUID uuid = UUID.fromString(eventId);
			Budget existing = budgetService.getByEventId(uuid)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
			
			// Validate ETag if provided
			if (ifMatch != null && !ifMatch.equals(BudgetHttpUtil.generateETag(existing))) {
				throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Budget has been modified.");
			}

			Budget updated = budgetService.updateBudget(existing.getId(), request);
			return BudgetHttpUtil.wrapResponse(updated, BudgetDetailResponse.fromEntity(updated));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ID format or data", e);
		}
	}

	// ==================== CATEGORY MANAGEMENT ====================

	@GetMapping("/categories")
    @RequiresPermission(value = RbacPermissions.BUDGET_READ, resources = {"event_id=#eventId"})
	@Operation(summary = "Get all categories", description = "Retrieve all categories for the event budget")
	public ResponseEntity<List<BudgetCategoryResponse>> getCategories(
			@PathVariable String eventId) {
		try {
			UUID eventUuid = UUID.fromString(eventId);
			Budget budget = budgetService.getOrCreateByEventId(eventUuid);
			List<BudgetCategory> categories = budgetService.listCategories(budget.getId());
			return ResponseEntity.ok(categories.stream().map(BudgetCategoryResponse::fromEntity).toList());
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ID format", e);
		}
	}

	// ==================== LINE ITEM MANAGEMENT (EXPENSES) ====================

	@PatchMapping("/line-items/auto-save")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_UPDATE, resources = {"event_id=#eventId"})
	@Operation(summary = "Auto-save expense draft", description = "Create or update a line item as draft. If id is null, creates new. If id is provided, updates existing.")
	public ResponseEntity<BudgetLineItemResponse> autoSaveDraft(
			@PathVariable String eventId,
			@Valid @RequestBody BudgetLineItemAutoSaveRequest request) {
		try {
			UUID eventUuid = UUID.fromString(eventId);
			Budget budget = budgetService.getOrCreateByEventId(eventUuid);
			BudgetLineItem saved = budgetService.autoSaveLineItemDraft(budget.getId(), request);
			return ResponseEntity.ok(BudgetLineItemResponse.fromEntity(saved));
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
	}

	@GetMapping("/line-items")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_READ, resources = {"event_id=#eventId"})
	@Operation(summary = "Get all expenses", description = "Retrieve all line items for the event budget")
	public ResponseEntity<List<BudgetLineItemResponse>> getLineItems(
			@PathVariable String eventId) {
		try {
			UUID eventUuid = UUID.fromString(eventId);
			Budget budget = budgetService.getOrCreateByEventId(eventUuid);
			List<BudgetLineItem> items = budgetService.listLineItems(budget.getId());
			return ResponseEntity.ok(items.stream().map(BudgetLineItemResponse::fromEntity).toList());
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
	}

	@DeleteMapping("/line-items/{itemId}")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_DELETE, resources = {"event_id=#eventId"})
	@Operation(summary = "Delete expense", description = "Remove a line item")
	public ResponseEntity<Void> deleteLineItem(
			@PathVariable String eventId,
			@PathVariable String itemId) {
		try {
			UUID itemUuid = UUID.fromString(itemId);
			budgetService.deleteLineItem(itemUuid);
			return ResponseEntity.noContent().build();
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
	}

	@PutMapping("/line-items/{itemId}/finalize")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_UPDATE, resources = {"event_id=#eventId"})
	@Operation(summary = "Finalize expense", description = "Publish a draft line item. Deletes the item if it has no content.")
	public ResponseEntity<BudgetLineItemResponse> finalizeLineItem(
			@PathVariable String eventId,
			@PathVariable String itemId,
			@Valid @RequestBody BudgetLineItemAutoSaveRequest request) {
		try {
			UUID itemUuid = UUID.fromString(itemId);
			return budgetService.finalizeLineItem(itemUuid, request)
					.map(finalized -> ResponseEntity.ok(BudgetLineItemResponse.fromEntity(finalized)))
					.orElseGet(() -> ResponseEntity.noContent().build());
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
	}
}
