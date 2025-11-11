package eventplanner.features.budget.controller;

import eventplanner.features.budget.dto.BudgetLineItemCreateRequest;
import eventplanner.features.budget.dto.BudgetUpsertRequest;
import eventplanner.features.budget.dto.request.*;
import eventplanner.features.budget.dto.response.*;
import eventplanner.features.budget.entity.Budget;
import eventplanner.features.budget.entity.BudgetLineItem;
import eventplanner.features.budget.service.BudgetService;
import eventplanner.features.budget.service.BudgetIdempotencyService;
import eventplanner.features.budget.service.BudgetRateLimitService;
import eventplanner.features.budget.service.BudgetExportService;
import eventplanner.features.budget.service.BudgetImportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;
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
@RequestMapping("/api/v1/budgets")
@Tag(name = "Budget Management", description = "Comprehensive budget management operations")
public class BudgetController {

	private final BudgetService budgetService;
	private final BudgetIdempotencyService idempotencyService;
	private final BudgetRateLimitService rateLimitService;
	private final BudgetExportService exportService;
	private final BudgetImportService importService;
	private final ObjectMapper objectMapper;

	public BudgetController(BudgetService budgetService, 
	                        BudgetIdempotencyService idempotencyService,
	                        BudgetRateLimitService rateLimitService,
	                        BudgetExportService exportService,
	                        BudgetImportService importService,
	                        ObjectMapper objectMapper) {
		this.budgetService = budgetService;
		this.idempotencyService = idempotencyService;
		this.rateLimitService = rateLimitService;
		this.exportService = exportService;
		this.importService = importService;
		this.objectMapper = objectMapper;
	}

	// ==================== CORE BUDGET CRUD ====================

	@GetMapping("/{eventId}")
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
			return budgetService.getByEventId(uuid)
				.map(budget -> {
					// Verify the user has access to this budget (ownership check already handled by RBAC)
					BudgetDetailResponse response = convertToDetailResponse(budget);
					return withETag(budget, response);
				})
				.orElseGet(() -> ResponseEntity.notFound().build());
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event ID format", e);
		}
	}

	@PostMapping
    @RequiresPermission(value = RbacPermissions.BUDGET_CREATE, resources = {"event_id=#budget.eventId"})
	@Operation(summary = "Create budget", description = "Create a new budget for an event")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "201", description = "Budget created successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid request data"),
		@ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated"),
		@ApiResponse(responseCode = "409", description = "Budget already exists for this event")
	})
	public ResponseEntity<BudgetDetailResponse> createBudget(
			@Valid @RequestBody BudgetUpsertRequest budget,
			@AuthenticationPrincipal UserPrincipal user) {
		if (budget == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget data is required");
		}
		
		if (user == null || user.getUser() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
		}
		
		// Check if budget already exists for this event
		if (budgetService.getByEventId(budget.getEventId()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, 
				"Budget already exists for this event. Use PUT to update.");
		}
		
		Budget entity = new Budget();
		entity.setEventId(budget.getEventId());
		entity.setOwnerId(user.getUser().getId());
		entity.setTotalBudget(budget.getTotalBudget());
		entity.setCurrency(budget.getCurrency());
		entity.setContingencyPercentage(budget.getContingencyPercentage());
		entity.setNotes(budget.getNotes());
		
		Budget saved = budgetService.createOrUpdate(entity);
		return ResponseEntity.status(HttpStatus.CREATED)
				.header("Location", "/api/v1/budgets/events/" + saved.getEventId() + "/budgets/" + saved.getId())
				.body(convertToDetailResponse(saved));
	}

	@PutMapping("/events/{eventId}/budgets/{budgetId}")
    @RequiresPermission(value = RbacPermissions.BUDGET_UPDATE, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Update budget details", description = "Update budget information including contingency settings")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget updated successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request data"),
		@ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated"),
		@ApiResponse(responseCode = "409", description = "Conflict - invalid status transition or version mismatch"),
		@ApiResponse(responseCode = "412", description = "Precondition Failed - ETag mismatch")
	})
	public ResponseEntity<BudgetDetailResponse> updateBudget(
			@PathVariable String eventId,
			@PathVariable String budgetId,
			@Valid @RequestBody UpdateBudgetRequest request,
			@RequestHeader(value = "If-Match", required = false) String ifMatch,
			@AuthenticationPrincipal UserPrincipal user) {
		if (user == null || user.getUser() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
		}
		
		try {
			UUID uuid = UUID.fromString(budgetId);
			
			// Verify ownership before updating
			Budget existing = budgetService.getById(uuid)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
			
			if (!existing.getOwnerId().equals(user.getUser().getId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to update this budget");
			}

			// Validate ETag if provided
			if (ifMatch != null && !ifMatch.equals(generateETag(existing))) {
				throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, 
					"Budget has been modified. Please refresh and try again.");
			}

			// Validate status transition if status is being changed
			if (request.getBudgetStatus() != null) {
				validateStatusTransition(existing.getBudgetStatus(), request.getBudgetStatus());
			}
			
			Budget updated = budgetService.updateBudget(uuid, request);
			BudgetDetailResponse response = convertToDetailResponse(updated);
			return withETag(updated, response);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format or data", e);
		} catch (RuntimeException e) {
			if (e.getMessage().contains("not found")) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
			}
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
		}
	}

	@DeleteMapping("/events/{eventId}/budgets/{budgetId}")
    @RequiresPermission(value = RbacPermissions.BUDGET_DELETE, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Delete budget", description = "Permanently delete a budget and all its line items")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "Budget deleted successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated"),
		@ApiResponse(responseCode = "403", description = "Forbidden - not the budget owner")
	})
	public ResponseEntity<Void> deleteBudget(
			@PathVariable String eventId,
			@PathVariable String budgetId,
			@AuthenticationPrincipal UserPrincipal user) {
		if (user == null || user.getUser() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
		}
		
		try {
			UUID uuid = UUID.fromString(budgetId);
			
			// Verify ownership before deleting
			Budget existing = budgetService.getById(uuid)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
			
			if (!existing.getOwnerId().equals(user.getUser().getId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this budget");
			}
			
			budgetService.deleteBudget(uuid);
			return ResponseEntity.noContent().build();
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		}
	}

	// ==================== LINE ITEM MANAGEMENT ====================

	@PostMapping("/events/{eventId}/budgets/{budgetId}/line-items")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_CREATE, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Add budget line item", description = "Add a single line item to the budget")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Line item added successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	public ResponseEntity<BudgetLineItemResponse> addLineItem(
			@PathVariable String eventId,
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
            item.setQuantity(payload.getQuantity());
			
			BudgetLineItem saved = budgetService.addLineItem(item);
			return ResponseEntity.ok(convertToLineItemResponse(saved));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		}
	}

	@PostMapping("/events/{eventId}/budgets/{budgetId}/line-items/bulk")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_CREATE, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Add multiple line items", description = "Add multiple line items to the budget in one operation with idempotency support")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Line items added successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request data or size exceeded"),
		@ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded")
	})
	public ResponseEntity<List<BudgetLineItemResponse>> addBulkLineItems(
			@PathVariable String eventId,
			@PathVariable String budgetId,
			@Valid @RequestBody BulkLineItemRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@AuthenticationPrincipal UserPrincipal user) {
		try {
			// Validate request size
			if (!rateLimitService.validateBulkSize(request.getLineItems().size())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
					String.format("Bulk request size exceeds maximum allowed (%d items)", 
						rateLimitService.getMaxBulkSize()));
			}
			
			// Check idempotency - return cached result if available
			if (idempotencyKey != null) {
				var cachedResult = idempotencyService.getProcessedResult(idempotencyKey);
				if (cachedResult.isPresent()) {
					List<BudgetLineItemResponse> cached = objectMapper.readValue(
						cachedResult.get(), 
						objectMapper.getTypeFactory().constructCollectionType(List.class, BudgetLineItemResponse.class)
					);
					return ResponseEntity.ok()
							.header("X-Idempotency-Replay", "true")
							.body(cached);
				}
			}
			
			// Check rate limit
			if (user != null && !rateLimitService.isAllowed(user.getUser().getId().toString())) {
				int remaining = rateLimitService.getRemainingRequests(user.getUser().getId().toString());
				throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, 
					String.format("Rate limit exceeded. Try again later. Remaining requests: %d", remaining));
			}
			
			UUID budgetUuid = UUID.fromString(budgetId);
			request.setBudgetId(budgetUuid);
			
			List<BudgetLineItem> saved = budgetService.addBulkLineItems(request);
			List<BudgetLineItemResponse> responses = saved.stream()
					.map(this::convertToLineItemResponse)
					.toList();
			
			// Store result for idempotency
			if (idempotencyKey != null) {
				try {
					String resultJson = objectMapper.writeValueAsString(responses);
					idempotencyService.storeResult(idempotencyKey, resultJson);
				} catch (Exception e) {
					// Log but don't fail the request
					System.err.println("Failed to store idempotency result: " + e.getMessage());
				}
			}
			
			return ResponseEntity.ok()
					.header("X-RateLimit-Remaining", 
						String.valueOf(rateLimitService.getRemainingRequests(user.getUser().getId().toString())))
					.body(responses);
		} catch (ResponseStatusException e) {
			throw e; // Re-throw status exceptions
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
				"Failed to process bulk line items", e);
		}
	}

	@GetMapping("/events/{eventId}/budgets/{budgetId}/line-items")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_READ, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Get all line items", description = "Retrieve all line items for a budget")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Line items retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<List<BudgetLineItemResponse>> getLineItems(
			@PathVariable String eventId,
			@PathVariable String budgetId) {
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

	@GetMapping("/events/{eventId}/budgets/{budgetId}/line-items/{itemId}")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_READ, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Get specific line item", description = "Retrieve a specific line item by ID")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Line item found"),
		@ApiResponse(responseCode = "404", description = "Line item not found")
	})
	public ResponseEntity<BudgetLineItemResponse> getLineItem(
			@PathVariable String eventId,
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

	@PutMapping("/events/{eventId}/budgets/{budgetId}/line-items/{itemId}")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_UPDATE, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Update line item", description = "Update an existing line item")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Line item updated successfully"),
		@ApiResponse(responseCode = "404", description = "Line item not found"),
		@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	public ResponseEntity<BudgetLineItemResponse> updateLineItem(
			@PathVariable String eventId,
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

	@DeleteMapping("/events/{eventId}/budgets/{budgetId}/line-items/{itemId}")
    @RequiresPermission(value = RbacPermissions.BUDGET_LINEITEM_DELETE, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Delete line item", description = "Remove a line item from the budget")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "204", description = "Line item deleted successfully"),
		@ApiResponse(responseCode = "404", description = "Line item not found")
	})
	public ResponseEntity<Void> deleteLineItem(
			@PathVariable String eventId,
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

	@GetMapping("/events/{eventId}/budgets/{budgetId}/summary")
    @RequiresPermission(value = RbacPermissions.BUDGET_READ, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Get budget summary", description = "Get comprehensive budget summary with key metrics")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<BudgetSummaryResponse> getBudgetSummary(
			@PathVariable String eventId,
			@PathVariable String budgetId) {
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

	@GetMapping("/events/{eventId}/budgets/{budgetId}/variance-analysis")
    @RequiresPermission(value = RbacPermissions.BUDGET_ANALYTICS_READ, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Get variance analysis", description = "Get detailed variance analysis by category")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Analysis retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<BudgetVarianceAnalysisResponse> getVarianceAnalysis(
			@PathVariable String eventId,
			@PathVariable String budgetId) {
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

	@GetMapping("/events/{eventId}/budgets/{budgetId}/contingency-analysis")
    @RequiresPermission(value = RbacPermissions.BUDGET_ANALYTICS_READ, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Get contingency analysis", description = "Get contingency usage and recommendations")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Analysis retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<BudgetContingencyResponse> getContingencyAnalysis(
			@PathVariable String eventId,
			@PathVariable String budgetId) {
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

	@GetMapping("/events/{eventId}/budgets/{budgetId}/category-breakdown")
    @RequiresPermission(value = RbacPermissions.BUDGET_ANALYTICS_READ, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Get category breakdown", description = "Get spending breakdown by category")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Breakdown retrieved successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<CategoryBreakdownResponse> getCategoryBreakdown(
			@PathVariable String eventId,
			@PathVariable String budgetId) {
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

	@PostMapping("/events/{eventId}/budgets/{budgetId}/submit-for-approval")
    @RequiresPermission(value = RbacPermissions.BUDGET_SUBMIT, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Submit budget for approval", description = "Submit budget for approval workflow")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget submitted for approval"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "409", description = "Budget cannot be submitted in current status")
	})
	public ResponseEntity<BudgetDetailResponse> submitForApproval(
			@PathVariable String eventId,
			@PathVariable String budgetId) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			
			// Validate status transition before attempting
			Budget existing = budgetService.getById(uuid)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
			validateStatusTransition(existing.getBudgetStatus(), "SUBMITTED");
			
			Budget updated = budgetService.submitForApproval(uuid);
			BudgetDetailResponse response = convertToDetailResponse(updated);
			return withETag(updated, response);
        } catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (ResponseStatusException e) {
			throw e; // Re-throw status exceptions
		} catch (RuntimeException e) {
			if (e.getMessage().contains("not found")) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
			} else {
				throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
			}
		}
	}

	@PostMapping("/events/{eventId}/budgets/{budgetId}/approve")
    @RequiresPermission(value = RbacPermissions.BUDGET_APPROVE, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Approve budget", description = "Approve the budget")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget approved successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "409", description = "Budget cannot be approved in current status")
	})
	public ResponseEntity<BudgetDetailResponse> approveBudget(
			@PathVariable String eventId,
			@PathVariable String budgetId,
			@Valid @RequestBody BudgetApprovalRequest request) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			
			// Validate status transition before attempting
			Budget existing = budgetService.getById(uuid)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
			validateStatusTransition(existing.getBudgetStatus(), "APPROVED");
			
			Budget updated = budgetService.approveBudget(uuid, request);
			BudgetDetailResponse response = convertToDetailResponse(updated);
			return withETag(updated, response);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (ResponseStatusException e) {
			throw e; // Re-throw status exceptions
		} catch (RuntimeException e) {
			if (e.getMessage().contains("not found")) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
			} else {
				throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
			}
		}
	}

	@PostMapping("/events/{eventId}/budgets/{budgetId}/reject")
    @RequiresPermission(value = RbacPermissions.BUDGET_REJECT, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Reject budget", description = "Reject the budget with reason")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget rejected successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "409", description = "Budget cannot be rejected in current status")
	})
	public ResponseEntity<BudgetDetailResponse> rejectBudget(
			@PathVariable String eventId,
			@PathVariable String budgetId,
			@Valid @RequestBody BudgetApprovalRequest request) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			
			// Validate status transition before attempting
			Budget existing = budgetService.getById(uuid)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
			validateStatusTransition(existing.getBudgetStatus(), "REJECTED");
			
			Budget updated = budgetService.rejectBudget(uuid, request);
			BudgetDetailResponse response = convertToDetailResponse(updated);
			return withETag(updated, response);
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (ResponseStatusException e) {
			throw e; // Re-throw status exceptions
		} catch (RuntimeException e) {
			if (e.getMessage().contains("not found")) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
			} else {
				throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
			}
		}
	}

	// ==================== EXPORT/IMPORT ENDPOINTS ====================

	@GetMapping("/events/{eventId}/budgets/{budgetId}/export/csv")
    @RequiresPermission(value = RbacPermissions.BUDGET_READ, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Export budget to CSV", description = "Export budget and all line items to CSV format")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget exported successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<byte[]> exportToCSV(
			@PathVariable String eventId,
			@PathVariable String budgetId) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			Budget budget = budgetService.getById(uuid)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
			
			byte[] csvData = exportService.exportToCSV(budget);
			
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("text/csv"));
			headers.setContentDispositionFormData("attachment", "budget_" + budgetId + ".csv");
			
			return ResponseEntity.ok()
					.headers(headers)
					.body(csvData);
					
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		}
	}

	@GetMapping("/events/{eventId}/budgets/{budgetId}/export/pdf")
    @RequiresPermission(value = RbacPermissions.BUDGET_READ, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Export budget to PDF", description = "Export budget and all line items to PDF format")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Budget exported successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found"),
		@ApiResponse(responseCode = "501", description = "PDF export not yet implemented")
	})
	public ResponseEntity<byte[]> exportToPDF(
			@PathVariable String eventId,
			@PathVariable String budgetId) {
		// PDF export requires additional dependencies (e.g., iText, Apache PDFBox)
		// For now, return CSV data with PDF mime type as placeholder
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, 
			"PDF export requires additional configuration. Please use CSV export.");
	}

	@PostMapping("/events/{eventId}/budgets/{budgetId}/import")
    @RequiresPermission(value = RbacPermissions.BUDGET_UPDATE, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Import line items from CSV", description = "Import budget line items from CSV file")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Line items imported successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid file format or data"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<List<BudgetLineItemResponse>> importFromCSV(
			@PathVariable String eventId,
			@PathVariable String budgetId,
			@RequestParam("file") MultipartFile file,
			@AuthenticationPrincipal UserPrincipal user) {
		try {
			if (file.isEmpty()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
			}
			
			if (!file.getOriginalFilename().endsWith(".csv")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
					"Only CSV files are supported");
			}
			
			UUID budgetUuid = UUID.fromString(budgetId);
			
			// Verify budget exists
			budgetService.getById(budgetUuid)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
			
			// Import line items
			BulkLineItemRequest request = importService.importFromCSV(file, budgetUuid);
			List<BudgetLineItem> saved = budgetService.addBulkLineItems(request);
			
			List<BudgetLineItemResponse> responses = saved.stream()
					.map(this::convertToLineItemResponse)
					.toList();
			
			return ResponseEntity.ok(responses);
			
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		} catch (ResponseStatusException e) {
			throw e;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
				"Failed to import line items: " + e.getMessage(), e);
		}
	}

	@GetMapping("/template")
	@Operation(summary = "Download import template", description = "Download CSV template with standardized categories for importing budget line items")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Template downloaded successfully")
	})
	public ResponseEntity<byte[]> downloadImportTemplate() {
		byte[] template = exportService.generateImportTemplate();
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("text/csv"));
		headers.setContentDispositionFormData("attachment", "budget_import_template.csv");
		
		return ResponseEntity.ok()
				.headers(headers)
				.body(template);
	}

	// ==================== UTILITY ENDPOINTS ====================

	@GetMapping("/events/{eventId}/budgets/{budgetId}/rollup")
    @RequiresPermission(value = RbacPermissions.BUDGET_ANALYTICS_READ, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Compute rollup total", description = "Compute total rollup for a budget")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Rollup computed successfully"),
		@ApiResponse(responseCode = "400", description = "Invalid budget ID format")
	})
	public ResponseEntity<java.math.BigDecimal> rollup(
			@PathVariable String eventId,
			@PathVariable String budgetId) {
		try {
			UUID uuid = UUID.fromString(budgetId);
			return ResponseEntity.ok(budgetService.computeRollup(uuid));
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid budget ID format", e);
		}
	}

	@PostMapping("/events/{eventId}/budgets/{budgetId}/recalculate")
    @RequiresPermission(value = RbacPermissions.BUDGET_RECALCULATE, resources = {"event_id=#eventId", "budget_id=#budgetId"})
	@Operation(summary = "Recalculate totals", description = "Force recalculation of budget totals and variance")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Totals recalculated successfully"),
		@ApiResponse(responseCode = "404", description = "Budget not found")
	})
	public ResponseEntity<BudgetDetailResponse> recalculateTotals(
			@PathVariable String eventId,
			@PathVariable String budgetId) {
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
	@Operation(summary = "Get standard budget categories", description = "Get list of standard budget categories (accessible to all authenticated users)")
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

	// ==================== UTILITY METHODS ====================

	/**
	 * Generate ETag from entity version and update timestamp
	 */
	private String generateETag(Budget budget) {
		if (budget.getVersion() == null) {
			return String.valueOf(budget.getUpdatedAt().hashCode());
		}
		return String.valueOf(budget.getVersion()) + "-" + budget.getUpdatedAt().hashCode();
	}


	/**
	 * Validate budget status transitions
	 * DRAFT -> SUBMITTED -> APPROVED/REJECTED
	 * APPROVED -> DRAFT (reopen)
	 * REJECTED -> DRAFT (reopen)
	 */
	private void validateStatusTransition(String currentStatus, String newStatus) {
		if (currentStatus == null) {
			currentStatus = "DRAFT";
		}
		if (newStatus == null || currentStatus.equals(newStatus)) {
			return; // No transition
		}

		boolean isValid = switch (currentStatus) {
			case "DRAFT" -> "SUBMITTED".equals(newStatus);
			case "SUBMITTED" -> "APPROVED".equals(newStatus) || "REJECTED".equals(newStatus);
			case "APPROVED", "REJECTED" -> "DRAFT".equals(newStatus); // Allow reopening
			default -> false;
		};

		if (!isValid) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
				String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
		}
	}

	/**
	 * Apply ETag header to response
	 */
	private ResponseEntity<BudgetDetailResponse> withETag(Budget budget, BudgetDetailResponse response) {
		return ResponseEntity.ok()
				.eTag(generateETag(budget))
				.body(response);
	}
}
