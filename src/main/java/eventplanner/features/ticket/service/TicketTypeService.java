package eventplanner.features.ticket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eventplanner.common.exception.ApiException;
import eventplanner.common.exception.ResourceNotFoundException;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.ticket.dto.request.CreateTicketTypeRequest;
import eventplanner.features.ticket.dto.request.PromotionDetails;
import eventplanner.features.ticket.dto.request.UpdateTicketTypeRequest;
import eventplanner.features.ticket.dto.response.TicketTypeResponse;
import eventplanner.features.ticket.entity.TicketPromotion;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.enums.TicketTypeCategory;
import eventplanner.features.ticket.repository.TicketPromotionRepository;
import eventplanner.features.ticket.repository.TicketTypeRepository;
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

    /**
     * Create a new ticket type for an event.
     */
    @Transactional
    public TicketType createTicketType(UUID eventId, CreateTicketTypeRequest request, UserPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
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

        // Serialize metadata if provided
        if (request.getMetadata() != null) {
            try {
                ticketType.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
            } catch (JsonProcessingException e) {

                throw new IllegalArgumentException("Invalid metadata format");
            }
        }

        TicketType saved = repository.save(ticketType);

        handlePromotionCreate(request, saved, principal);
        
        return saved;
    }

    /**
     * Update an existing ticket type with optimistic locking.
     */
    @Transactional
    public TicketType updateTicketType(UUID id, UUID eventId, UpdateTicketTypeRequest request, Long expectedVersion, UserPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
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
                throw new IllegalArgumentException(
                    "Quantity available cannot be less than quantity sold (" + ticketType.getQuantitySold() + 
                    ") + quantity reserved (" + ticketType.getQuantityReserved() + ")");
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
        if (request.getMetadata() != null) {
            try {
                ticketType.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
            } catch (JsonProcessingException e) {

                throw new IllegalArgumentException("Invalid metadata format");
            }
        }

        TicketType saved = repository.save(ticketType);

        handlePromotionUpdate(request, saved, principal);
        
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
            throw new IllegalArgumentException("Cannot delete ticket type with issued tickets: " + issuedCount);
        }

        ticketType.softDelete();
        repository.save(ticketType);

    }

    /**
     * Get ticket types for an event with optional filters.
     * Supports filtering by ID, category, active status, and name.
     * 
     * @param eventId Event ID (required)
     * @param id Optional ticket type ID to get a specific ticket type
     * @param category Optional category filter
     * @param activeOnly Filter to only active ticket types
     * @param name Optional name filter (partial match)
     * @return List of ticket type responses (single item if ID is provided)
     */
    @Transactional(readOnly = true)
    public List<TicketTypeResponse> getTicketTypes(UUID eventId, UUID id, TicketTypeCategory category, 
                                                    Boolean activeOnly, String name) {
        List<TicketType> ticketTypes;
        
        // If ID is provided, get specific ticket type
        if (id != null) {
            TicketType ticketType = repository.findByIdAndEventId(id, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket type not found: " + id + " for event: " + eventId));
            return List.of(TicketTypeResponse.from(ticketType));
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
        
        return ticketTypes.stream()
            .map(TicketTypeResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Get all ticket types for an event (backward compatibility).
     */
    @Transactional(readOnly = true)
    public List<TicketTypeResponse> getTicketTypesByEventId(UUID eventId, boolean activeOnly) {
        return getTicketTypes(eventId, null, null, activeOnly, null);
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
            throw new IllegalArgumentException(
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
            throw new IllegalArgumentException(
                "Cannot confirm sale of " + quantity + " tickets. Only " + ticketType.getQuantityReserved() + " are reserved");
        }

        int updated = repository.moveReservedToSold(ticketTypeId, quantity);
        if (updated == 0) {
            throw new ApiException("TICKET_TYPE_NOT_AVAILABLE", 
                "Failed to confirm sale - ticket type may have been modified", 409);
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
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }
        
        String normalized = currencyCode.trim().toUpperCase();
        try {
            Monetary.getCurrency(normalized);
            return normalized;
        } catch (Exception e) {
            throw new IllegalArgumentException(
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

}
