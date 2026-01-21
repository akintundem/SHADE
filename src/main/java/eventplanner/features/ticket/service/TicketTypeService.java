package eventplanner.features.ticket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eventplanner.common.exception.exceptions.ApiException;
import eventplanner.common.exception.exceptions.ErrorCode;
import eventplanner.common.exception.exceptions.ForbiddenException;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.common.exception.exceptions.BadRequestException;
import eventplanner.common.exception.exceptions.ConflictException;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.security.authorization.service.AuthorizationService;
import eventplanner.features.ticket.dto.request.CreateTicketTypeRequest;
import eventplanner.features.ticket.dto.request.CloneTicketTypeRequest;
import eventplanner.features.ticket.dto.request.PromotionDetails;
import eventplanner.features.ticket.dto.request.TicketPriceTierRequest;
import eventplanner.features.ticket.dto.request.TicketTypeDependencyRequest;
import eventplanner.features.ticket.dto.request.UpdateTicketTypeRequest;
import eventplanner.features.ticket.dto.response.TicketTypeResponse;
import eventplanner.features.ticket.dto.response.TicketPriceTierResponse;
import eventplanner.features.ticket.dto.response.TicketTypeDependencyResponse;
import eventplanner.features.ticket.entity.TicketPromotion;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.entity.TicketPriceTier;
import eventplanner.features.ticket.entity.TicketTypeDependency;
import eventplanner.features.ticket.enums.TicketTypeCategory;
import eventplanner.features.ticket.repository.TicketPromotionRepository;
import eventplanner.features.ticket.repository.TicketTypeRepository;
import eventplanner.features.ticket.repository.TicketPriceTierRepository;
import eventplanner.features.ticket.repository.TicketTypeDependencyRepository;
import eventplanner.security.auth.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.money.Monetary;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for ticket type management operations.
 * Handles creation, updates, deletion, and quantity management with atomic operations.
 */
@Service
@RequiredArgsConstructor

public class TicketTypeService {

    private final TicketTypeRepository repository;
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final TicketPromotionRepository ticketPromotionRepository;
    private final TicketPriceTierRepository ticketPriceTierRepository;
    private final TicketTypeDependencyRepository ticketTypeDependencyRepository;
    private final AuthorizationService authorizationService;

    /**
     * Create a new ticket type for an event.
     */
    @Transactional
    public TicketType createTicketType(UUID eventId, CreateTicketTypeRequest request, UserPrincipal principal) {
        if (request == null) {
            throw new BadRequestException("Request cannot be null");
        }

        // Fetch event
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        // Create ticket type
        TicketType ticketType = new TicketType();
        ticketType.setEvent(event);
        ticketType.setName(request.getName());
        ticketType.setCategory(request.getCategory());
        ticketType.setDescription(request.getDescription());
        ticketType.setPriceMinor(request.getPriceMinor());
        
        // Validate and set currency (normalized to uppercase)
        String currencyCode = request.getCurrency() != null ? request.getCurrency() : "USD";
        String normalizedCurrency = validateCurrencyCode(currencyCode);
        ticketType.setCurrency(normalizedCurrency);
        
        ticketType.setQuantityAvailable(request.getQuantityAvailable() != null ? request.getQuantityAvailable() : 0);
        ticketType.setQuantitySold(0);
        ticketType.setQuantityReserved(0);
        ticketType.setIsActive(true);
        ticketType.setSaleStartDate(request.getSaleStartDate());
        ticketType.setSaleEndDate(request.getSaleEndDate());
        ticketType.setMaxTicketsPerPerson(request.getMaxTicketsPerPerson());
        ticketType.setRequiresApproval(request.getRequiresApproval() != null ? request.getRequiresApproval() : false);
        ticketType.setEarlyBirdPriceMinor(request.getEarlyBirdPriceMinor());
        ticketType.setEarlyBirdEndDate(request.getEarlyBirdEndDate());
        ticketType.setGroupDiscountMinQuantity(request.getGroupDiscountMinQuantity());
        ticketType.setGroupDiscountPercentBps(request.getGroupDiscountPercentBps());

        // Serialize metadata if provided
        if (request.getMetadata() != null) {
            try {
                ticketType.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
            } catch (JsonProcessingException e) {

                throw new BadRequestException("Invalid metadata format");
            }
        }

        // Validate total ticket inventory doesn't exceed event capacity
        if (event.getCapacity() != null && request.getQuantityAvailable() != null) {
            long existingInventory = repository.findByEventId(eventId).stream()
                .mapToInt(TicketType::getQuantityAvailable)
                .sum();

            long totalInventory = existingInventory + request.getQuantityAvailable();

            if (totalInventory > event.getCapacity()) {
                throw new BadRequestException(
                    String.format("Total ticket inventory (%d) would exceed event capacity (%d). " +
                        "Current inventory: %d, Requested: %d",
                        totalInventory, event.getCapacity(), existingInventory, request.getQuantityAvailable()));
            }
        }

        TicketType saved = repository.save(ticketType);

        handlePromotionCreate(request, saved, principal);
        upsertPriceTiers(request.getPriceTiers(), saved);
        upsertDependencies(request.getDependencies(), saved);
        
        return saved;
    }

    /**
     * Update an existing ticket type with optimistic locking.
     */
    @Transactional
    public TicketType updateTicketType(UUID id, UUID eventId, UpdateTicketTypeRequest request, Long expectedVersion, UserPrincipal principal) {
        if (request == null) {
            throw new BadRequestException("Request cannot be null");
        }

        TicketType ticketType = repository.findByIdAndEventId(id, eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + id + " for event: " + eventId));

        // Check version if provided
        if (expectedVersion != null && !expectedVersion.equals(ticketType.getVersion())) {
            throw new OptimisticLockingFailureException(
                "Ticket type has been modified by another user. Current version: " + ticketType.getVersion());
        }

        // Update fields
        if (request.getName() != null) {
            ticketType.setName(request.getName());
        }
        if (request.getCategory() != null) {
            ticketType.setCategory(request.getCategory());
        }
        if (request.getDescription() != null) {
            ticketType.setDescription(request.getDescription());
        }
        if (request.getPriceMinor() != null) {
            ticketType.setPriceMinor(request.getPriceMinor());
        }
        if (request.getCurrency() != null) {
            String normalizedCurrency = validateCurrencyCode(request.getCurrency());
            ticketType.setCurrency(normalizedCurrency);
        }
        if (request.getQuantityAvailable() != null) {
            // Validate that new quantity is not less than sold + reserved
            int minRequired = ticketType.getQuantitySold() + ticketType.getQuantityReserved();
            if (request.getQuantityAvailable() < minRequired) {
                throw new BadRequestException(
                    "Quantity available cannot be less than quantity sold (" + ticketType.getQuantitySold() +
                    ") + quantity reserved (" + ticketType.getQuantityReserved() + ")");
            }

            // Validate total ticket inventory doesn't exceed event capacity (when updating)
            Event event = ticketType.getEvent();
            if (event.getCapacity() != null) {
                long existingInventory = repository.findByEventId(eventId).stream()
                    .filter(tt -> !tt.getId().equals(id)) // Exclude current ticket type
                    .mapToInt(TicketType::getQuantityAvailable)
                    .sum();

                long totalInventory = existingInventory + request.getQuantityAvailable();

                if (totalInventory > event.getCapacity()) {
                    throw new BadRequestException(
                        String.format("Total ticket inventory (%d) would exceed event capacity (%d). " +
                            "Other ticket types: %d, Requested: %d",
                            totalInventory, event.getCapacity(), existingInventory, request.getQuantityAvailable()));
                }
            }

            ticketType.setQuantityAvailable(request.getQuantityAvailable());
        }
        if (request.getSaleStartDate() != null) {
            ticketType.setSaleStartDate(request.getSaleStartDate());
        }
        if (request.getSaleEndDate() != null) {
            ticketType.setSaleEndDate(request.getSaleEndDate());
        }
        if (request.getMaxTicketsPerPerson() != null) {
            ticketType.setMaxTicketsPerPerson(request.getMaxTicketsPerPerson());
        }
        if (request.getIsActive() != null) {
            ticketType.setIsActive(request.getIsActive());
        }
        if (request.getRequiresApproval() != null) {
            ticketType.setRequiresApproval(request.getRequiresApproval());
        }
        if (request.getEarlyBirdPriceMinor() != null) {
            ticketType.setEarlyBirdPriceMinor(request.getEarlyBirdPriceMinor());
        }
        if (request.getEarlyBirdEndDate() != null) {
            ticketType.setEarlyBirdEndDate(request.getEarlyBirdEndDate());
        }
        if (request.getGroupDiscountMinQuantity() != null) {
            ticketType.setGroupDiscountMinQuantity(request.getGroupDiscountMinQuantity());
        }
        if (request.getGroupDiscountPercentBps() != null) {
            ticketType.setGroupDiscountPercentBps(request.getGroupDiscountPercentBps());
        }
        if (request.getMetadata() != null) {
            try {
                ticketType.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
            } catch (JsonProcessingException e) {

                throw new BadRequestException("Invalid metadata format");
            }
        }

        TicketType saved = repository.save(ticketType);

        handlePromotionUpdate(request, saved, principal);
        upsertPriceTiers(request.getPriceTiers(), saved);
        upsertDependencies(request.getDependencies(), saved);
        
        return saved;
    }

    /**
     * Delete (soft delete) a ticket type.
     */
    @Transactional
    public void deleteTicketType(UUID id, UUID eventId, UserPrincipal principal) {
        TicketType ticketType = repository.findByIdAndEventId(id, eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + id + " for event: " + eventId));

        // Prevent deletion when tickets already exist
        long issuedCount = repository.countTicketsByTicketTypeId(id);
        if (issuedCount > 0) {
            throw new BadRequestException("Cannot delete ticket type with issued tickets: " + issuedCount);
        }

        ticketType.softDelete();
        repository.save(ticketType);

    }

    /**
     * Archive a ticket type (mark inactive).
     */
    @Transactional
    public TicketType archiveTicketType(UUID id, UUID eventId, UserPrincipal principal) {
        TicketType ticketType = repository.findByIdAndEventId(id, eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + id + " for event: " + eventId));
        ticketType.setIsActive(false);
        return repository.save(ticketType);
    }

    /**
     * Restore a previously archived ticket type (mark active).
     */
    @Transactional
    public TicketType restoreTicketType(UUID id, UUID eventId, UserPrincipal principal) {
        TicketType ticketType = repository.findByIdAndEventId(id, eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + id + " for event: " + eventId));
        ticketType.setIsActive(true);
        return repository.save(ticketType);
    }

    /**
     * Permanently delete a ticket type (hard delete).
     */
    @Transactional
    public void hardDeleteTicketType(UUID id, UUID eventId, UserPrincipal principal) {
        long issuedCount = repository.countTicketsByTicketTypeId(id);
        if (issuedCount > 0) {
            throw new BadRequestException("Cannot hard delete ticket type with issued tickets: " + issuedCount);
        }
        int deleted = repository.hardDeleteByIdAndEventId(id, eventId);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Ticket type not found: " + id + " for event: " + eventId);
        }
    }

    /**
     * Clone a ticket type within the same event.
     */
    @Transactional
    public TicketType cloneTicketType(UUID id, UUID eventId, CloneTicketTypeRequest request, UserPrincipal principal) {
        TicketType source = repository.findByIdAndEventId(id, eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + id + " for event: " + eventId));

        TicketType clone = new TicketType();
        clone.setEvent(source.getEvent());
        String cloneName = request != null && request.getName() != null && !request.getName().isBlank()
            ? request.getName().trim()
            : source.getName() + " (Copy)";
        clone.setName(cloneName);
        clone.setCategory(source.getCategory());
        clone.setDescription(source.getDescription());
        clone.setPriceMinor(source.getPriceMinor());
        clone.setCurrency(source.getCurrency());
        clone.setQuantityAvailable(source.getQuantityAvailable());
        clone.setQuantitySold(0);
        clone.setQuantityReserved(0);
        clone.setSaleStartDate(source.getSaleStartDate());
        clone.setSaleEndDate(source.getSaleEndDate());
        clone.setMaxTicketsPerPerson(source.getMaxTicketsPerPerson());
        clone.setRequiresApproval(source.getRequiresApproval());
        clone.setEarlyBirdPriceMinor(source.getEarlyBirdPriceMinor());
        clone.setEarlyBirdEndDate(source.getEarlyBirdEndDate());
        clone.setGroupDiscountMinQuantity(source.getGroupDiscountMinQuantity());
        clone.setGroupDiscountPercentBps(source.getGroupDiscountPercentBps());
        clone.setMetadata(source.getMetadata());
        boolean active = request != null && request.getIsActive() != null ? request.getIsActive() : false;
        clone.setIsActive(active);

        TicketType saved = repository.save(clone);
        List<TicketPriceTier> tiers = ticketPriceTierRepository.findByTicketTypeIdIn(List.of(source.getId()));
        if (!tiers.isEmpty()) {
            for (TicketPriceTier tier : tiers) {
                TicketPriceTier clonedTier = new TicketPriceTier();
                clonedTier.setTicketType(saved);
                clonedTier.setName(tier.getName());
                clonedTier.setStartsAt(tier.getStartsAt());
                clonedTier.setEndsAt(tier.getEndsAt());
                clonedTier.setPriceMinor(tier.getPriceMinor());
                clonedTier.setPriority(tier.getPriority());
                ticketPriceTierRepository.save(clonedTier);
            }
        }
        List<TicketTypeDependency> deps = ticketTypeDependencyRepository.findByTicketTypeIdIn(List.of(source.getId()));
        if (!deps.isEmpty()) {
            for (TicketTypeDependency dep : deps) {
                TicketTypeDependency clonedDep = new TicketTypeDependency();
                clonedDep.setTicketType(saved);
                clonedDep.setRequiredTicketType(dep.getRequiredTicketType());
                clonedDep.setMinQuantity(dep.getMinQuantity());
                ticketTypeDependencyRepository.save(clonedDep);
            }
        }

        return saved;
    }

    /**
     * Get ticket types for an event with optional filters.
     * Supports filtering by ID, category, active status, and name.
     * Enforces access control for private events.
     * 
     * @param eventId Event ID (required)
     * @param id Optional ticket type ID to get a specific ticket type
     * @param category Optional category filter
     * @param activeOnly Filter to only active ticket types
     * @param name Optional name filter (partial match)
     * @param principal User principal for access control (required for private events)
     * @return List of ticket type responses (single item if ID is provided)
     * @throws ForbiddenException if user doesn't have access to private event
     */
    @Transactional(readOnly = true)
    public List<TicketTypeResponse> getTicketTypes(UUID eventId, UUID id, TicketTypeCategory category, 
                                                    Boolean activeOnly, String name, UserPrincipal principal) {
        // Fetch event to check access control
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        
        // Enforce access control for private events
        if (!Boolean.TRUE.equals(event.getIsPublic())) {
            if (principal == null) {
                throw new ForbiddenException("Authentication required to access private event ticket types");
            }
            if (!authorizationService.canAccessEventWithInvite(principal, event)) {
                throw new ForbiddenException("Access denied to private event ticket types");
            }
        }
        
        List<TicketType> ticketTypes;
        
        // If ID is provided, get specific ticket type
        if (id != null) {
            TicketType ticketType = repository.findByIdAndEventId(id, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + id + " for event: " + eventId));
            TicketTypeResponse response = TicketTypeResponse.from(ticketType);
            List<TicketPriceTierResponse> tierResponses = ticketPriceTierRepository
                .findByTicketTypeIdIn(List.of(ticketType.getId())).stream()
                .map(TicketPriceTierResponse::from)
                .collect(Collectors.toList());
            List<TicketTypeDependencyResponse> dependencyResponses = ticketTypeDependencyRepository
                .findByTicketTypeIdIn(List.of(ticketType.getId())).stream()
                .map(TicketTypeDependencyResponse::from)
                .collect(Collectors.toList());
            response.setPriceTiers(tierResponses);
            response.setDependencies(dependencyResponses);
            return List.of(response);
        }
        
        // Build query based on filters
        if (category != null && Boolean.TRUE.equals(activeOnly)) {
            ticketTypes = repository.findByEventIdAndCategoryAndIsActive(eventId, category);
        } else if (category != null) {
            ticketTypes = repository.findByEventIdAndCategory(eventId, category);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            ticketTypes = repository.findByEventIdAndIsActiveTrue(eventId);
        } else {
            ticketTypes = repository.findByEventId(eventId);
        }
        
        // Filter by name if provided (case-insensitive partial match)
        if (name != null && !name.trim().isEmpty()) {
            String nameLower = name.trim().toLowerCase();
            ticketTypes = ticketTypes.stream()
                .filter(tt -> tt.getName() != null && tt.getName().toLowerCase().contains(nameLower))
                .collect(Collectors.toList());
        }
        
        List<TicketTypeResponse> responses = ticketTypes.stream()
            .map(TicketTypeResponse::from)
            .collect(Collectors.toList());

        if (!responses.isEmpty()) {
            List<UUID> ids = ticketTypes.stream()
                .map(TicketType::getId)
                .toList();
            var tiers = ticketPriceTierRepository.findByTicketTypeIdIn(ids);
            var deps = ticketTypeDependencyRepository.findByTicketTypeIdIn(ids);

            var tiersByType = tiers.stream()
                .collect(Collectors.groupingBy(t -> t.getTicketType().getId()));
            var depsByType = deps.stream()
                .collect(Collectors.groupingBy(d -> d.getTicketType().getId()));

            for (TicketTypeResponse response : responses) {
                if (response.getId() == null) {
                    continue;
                }
                List<TicketPriceTierResponse> tierResponses = tiersByType
                    .getOrDefault(response.getId(), List.of()).stream()
                    .map(TicketPriceTierResponse::from)
                    .collect(Collectors.toList());
                List<TicketTypeDependencyResponse> dependencyResponses = depsByType
                    .getOrDefault(response.getId(), List.of()).stream()
                    .map(TicketTypeDependencyResponse::from)
                    .collect(Collectors.toList());
                response.setPriceTiers(tierResponses);
                response.setDependencies(dependencyResponses);
            }
        }

        return responses;
    }

    /**
     * Get all ticket types for an event (backward compatibility).
     * 
     * @param eventId Event ID (required)
     * @param activeOnly Filter to only active ticket types
     * @param principal User principal for access control (required for private events)
     * @return List of ticket type responses
     */
    @Transactional(readOnly = true)
    public List<TicketTypeResponse> getTicketTypesByEventId(UUID eventId, boolean activeOnly, UserPrincipal principal) {
        return getTicketTypes(eventId, null, null, activeOnly, null, principal);
    }

    /**
     * Get ticket type entity by ID (for internal use).
     */
    @Transactional(readOnly = true)
    public TicketType getTicketTypeByIdEntity(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + id));
    }

    /**
     * Reserve tickets atomically (for pending payments).
     * Used internally by TicketService when issuing paid tickets.
     */
    @Transactional
    public boolean reserveTickets(UUID ticketTypeId, int quantity) {
        TicketType ticketType = repository.findByIdForUpdate(ticketTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + ticketTypeId));

        if (!ticketType.canPurchase(quantity)) {
            return false;
        }

        int updated = repository.incrementQuantityReserved(ticketTypeId, quantity);
        return updated > 0;
    }

    /**
     * Release reserved tickets.
     * Used internally when ticket reservations expire or are cancelled.
     */
    @Transactional
    public boolean releaseReservation(UUID ticketTypeId, int quantity) {
        TicketType ticketType = repository.findByIdForUpdate(ticketTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + ticketTypeId));

        if (ticketType.getQuantityReserved() < quantity) {
            throw new BadRequestException(
                "Cannot release " + quantity + " tickets. Only " + ticketType.getQuantityReserved() + " are reserved");
        }

        int updated = repository.decrementQuantityReserved(ticketTypeId, quantity);
        return updated > 0;
    }

    /**
     * Confirm sale - move reserved tickets to sold.
     * Used internally when payment is confirmed for paid tickets.
     */
    @Transactional
    public void confirmSale(UUID ticketTypeId, int quantity) {
        TicketType ticketType = repository.findByIdForUpdate(ticketTypeId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + ticketTypeId));

        if (ticketType.getQuantityReserved() < quantity) {
            throw new BadRequestException(
                "Cannot confirm sale of " + quantity + " tickets. Only " + ticketType.getQuantityReserved() + " are reserved");
        }

        int updated = repository.moveReservedToSold(ticketTypeId, quantity);
        if (updated == 0) {
            throw new ApiException(ErrorCode.TICKET_TYPE_NOT_AVAILABLE, 
                "Failed to confirm sale - ticket type may have been modified");
        }
    }

    /**
     * Validate and normalize currency code using JavaMoney (JSR 354) - ISO 4217 standard.
     * This ensures compatibility with payment gateways like Stripe and PayPal.
     * 
     * @param currencyCode The currency code to validate
     * @return Normalized (uppercase) currency code
     * @throws IllegalArgumentException if the currency code is invalid
     */
    public String validateCurrencyCode(String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new BadRequestException("Currency code cannot be null or empty");
        }
        
        String normalized = currencyCode.trim().toUpperCase();
        try {
            Monetary.getCurrency(normalized);
            return normalized;
        } catch (Exception e) {
            throw new BadRequestException(
                "Invalid currency code: " + currencyCode + ". Must be a valid ISO 4217 code (e.g., USD, EUR, GBP).", e);
        }
    }

    private void handlePromotionCreate(CreateTicketTypeRequest request, TicketType ticketType, UserPrincipal principal) {
        upsertPromotions(request.getPromotions(), ticketType);
    }

    private void handlePromotionUpdate(UpdateTicketTypeRequest request, TicketType ticketType, UserPrincipal principal) {
        upsertPromotions(request.getPromotions(), ticketType);
    }

    private void upsertPromotions(List<PromotionDetails> promotions, TicketType ticketType) {
        if (promotions == null || promotions.isEmpty() || ticketType == null) {
            return;
        }
        UUID eventId = ticketType.getEvent() != null ? ticketType.getEvent().getId() : null;
        for (PromotionDetails promo : promotions) {
            if (promo == null || promo.getCode() == null) {
                continue;
            }
            String normalized = promo.getCode().trim().toUpperCase(Locale.ROOT);
            TicketPromotion target = ticketPromotionRepository
                .findByEventIdAndTicketTypeIdAndCodeIgnoreCase(eventId, ticketType.getId(), normalized)
                .orElse(null);
            if (target == null) {
                target = new TicketPromotion();
                target.setEvent(ticketType.getEvent());
                target.setTicketType(ticketType);
                target.setCode(normalized);
            }
            if (promo.getPercentOffBasisPoints() != null) {
                target.setPercentOffBasisPoints(promo.getPercentOffBasisPoints());
            }
            if (promo.getAmountOffMinor() != null) {
                target.setAmountOffMinor(promo.getAmountOffMinor());
            }
            if (promo.getStartsAt() != null) {
                target.setStartsAt(promo.getStartsAt());
            }
            if (promo.getEndsAt() != null) {
                target.setEndsAt(promo.getEndsAt());
            }
            if (promo.getActive() != null) {
                target.setIsActive(promo.getActive());
            }
            if ((target.getPercentOffBasisPoints() == null || target.getPercentOffBasisPoints() == 0) &&
                (target.getAmountOffMinor() == null || target.getAmountOffMinor() == 0)) {
                continue;
            }
            ticketPromotionRepository.save(target);
        }
    }

    private void upsertPriceTiers(List<TicketPriceTierRequest> tiers, TicketType ticketType) {
        if (ticketType == null) {
            return;
        }
        if (tiers == null) {
            return;
        }
        ticketPriceTierRepository.deleteByTicketTypeId(ticketType.getId());
        if (tiers.isEmpty()) {
            return;
        }
        for (TicketPriceTierRequest request : tiers) {
            if (request == null || request.getPriceMinor() == null) {
                continue;
            }
            TicketPriceTier tier = new TicketPriceTier();
            tier.setTicketType(ticketType);
            tier.setName(request.getName() != null ? request.getName().trim() : null);
            tier.setStartsAt(request.getStartsAt());
            tier.setEndsAt(request.getEndsAt());
            if (request.getStartsAt() != null && request.getEndsAt() != null &&
                request.getEndsAt().isBefore(request.getStartsAt())) {
                throw new BadRequestException("Tier end date must be after start date");
            }
            tier.setPriceMinor(request.getPriceMinor());
            tier.setPriority(request.getPriority() != null ? request.getPriority() : 0);
            ticketPriceTierRepository.save(tier);
        }
    }

    private void upsertDependencies(List<TicketTypeDependencyRequest> dependencies, TicketType ticketType) {
        if (ticketType == null) {
            return;
        }
        if (dependencies == null) {
            return;
        }
        ticketTypeDependencyRepository.deleteByTicketTypeId(ticketType.getId());
        if (dependencies.isEmpty()) {
            return;
        }
        for (TicketTypeDependencyRequest request : dependencies) {
            if (request == null || request.getRequiredTicketTypeId() == null) {
                continue;
            }
            if (request.getRequiredTicketTypeId().equals(ticketType.getId())) {
                throw new BadRequestException("Ticket type cannot depend on itself");
            }
            TicketType required = repository.findById(request.getRequiredTicketTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Required ticket type not found: " + request.getRequiredTicketTypeId()));
            if (required.getEvent() == null || ticketType.getEvent() == null ||
                !required.getEvent().getId().equals(ticketType.getEvent().getId())) {
                throw new BadRequestException("Required ticket type must belong to the same event");
            }
            TicketTypeDependency dependency = new TicketTypeDependency();
            dependency.setTicketType(ticketType);
            dependency.setRequiredTicketType(required);
            dependency.setMinQuantity(request.getMinQuantity() != null ? request.getMinQuantity() : 1);
            ticketTypeDependencyRepository.save(dependency);
        }
    }

}
