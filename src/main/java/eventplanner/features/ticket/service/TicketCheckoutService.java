package eventplanner.features.ticket.service;

import eventplanner.common.exception.exceptions.ApiException;
import eventplanner.common.exception.exceptions.ErrorCode;
import eventplanner.common.exception.exceptions.ResourceNotFoundException;
import eventplanner.features.event.entity.Event;
import eventplanner.features.event.repository.EventRepository;
import eventplanner.features.ticket.dto.request.CreateTicketCheckoutRequest;
import eventplanner.features.ticket.dto.request.IssueTicketRequest;
import eventplanner.features.ticket.dto.request.TicketCheckoutItemRequest;
import eventplanner.features.ticket.dto.response.TicketCheckoutResponse;
import eventplanner.features.ticket.dto.response.TicketResponse;
import eventplanner.features.ticket.dto.response.TicketPaymentInitResponse;
import eventplanner.features.ticket.entity.Ticket;
import eventplanner.features.ticket.entity.TicketCheckout;
import eventplanner.features.ticket.entity.TicketCheckoutItem;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.entity.TicketPromotion;
import eventplanner.features.ticket.entity.TicketPriceTier;
import eventplanner.features.ticket.entity.TicketTypeDependency;
import eventplanner.features.ticket.enums.TicketCheckoutStatus;
import eventplanner.features.ticket.enums.TicketStatus;
import eventplanner.features.ticket.repository.TicketCheckoutItemRepository;
import eventplanner.features.ticket.repository.TicketCheckoutRepository;
import eventplanner.features.ticket.repository.TicketRepository;
import eventplanner.features.ticket.repository.TicketTypeRepository;
import eventplanner.features.ticket.repository.TicketPromotionRepository;
import eventplanner.features.ticket.repository.TicketPriceTierRepository;
import eventplanner.features.ticket.repository.TicketTypeDependencyRepository;
import eventplanner.security.auth.entity.UserAccount;
import eventplanner.security.auth.repository.UserAccountRepository;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.auth.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Checkout flow for purchasing tickets without payment integration.
 * Holds tickets in PENDING status, provides cost breakdown, and finalizes or releases reservations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TicketCheckoutService {

    private static final int CHECKOUT_HOLD_MINUTES = 15;

    private final TicketCheckoutRepository checkoutRepository;
    private final TicketCheckoutItemRepository checkoutItemRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final TicketService ticketService;
    private final TicketTypeService ticketTypeService;
    private final UserAccountRepository userAccountRepository;
    private final TicketPromotionRepository ticketPromotionRepository;
    private final TicketPriceTierRepository ticketPriceTierRepository;
    private final TicketTypeDependencyRepository ticketTypeDependencyRepository;
    private final LocationRepository locationRepository;
    private final TicketingPolicyService ticketingPolicyService;

    /**
     * Start a new checkout session and reserve tickets.
     */
    public TicketCheckoutResponse createCheckout(UUID eventId, CreateTicketCheckoutRequest request, UserPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (principal == null || principal.getUser() == null) {
            throw new IllegalArgumentException("Authenticated user is required to start checkout");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one ticket item is required");
        }

        UserAccount purchaser = loadPurchaser(principal);
        if (purchaser.getEmail() == null || purchaser.getEmail().isBlank()) {
            throw new IllegalArgumentException("User account email is required for checkout");
        }

        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        ticketingPolicyService.ensureEventOpenForTicketing(event);

        List<TicketCheckoutItemRequest> normalizedItems = normalizeItems(request.getItems());
        Map<UUID, TicketType> ticketTypes = loadTicketTypes(normalizedItems, event.getId());
        String currency = determineCurrency(ticketTypes.values());
        Map<UUID, List<TicketPriceTier>> activeTiers = loadActivePriceTiers(ticketTypes.values());

        validateDependencies(event, purchaser, normalizedItems, ticketTypes);

        // Build issue requests; keep free tickets pending so checkout can finalize later.
        List<TicketCheckoutItem> checkoutItems = new ArrayList<>();
        List<IssueTicketRequest> issueRequests = new ArrayList<>();

        for (TicketCheckoutItemRequest item : normalizedItems) {
            TicketType ticketType = ticketTypes.get(item.getTicketTypeId());
            if (Boolean.TRUE.equals(ticketType.getRequiresApproval())) {
                throw new IllegalArgumentException("Ticket type requires approval");
            }

            IssueTicketRequest issueRequest =
                new IssueTicketRequest(
                    event.getId(),
                    ticketType.getId(),
                    null,
                    null,
                    null,
                    item.getQuantity(),
                    false,
                    false
                );
            issueRequests.add(issueRequest);

            long unitPriceMinor = ticketType.getPriceMinor() != null ? ticketType.getPriceMinor() : 0L;
            long lineTotalMinor = Math.multiplyExact(unitPriceMinor, item.getQuantity());

            TicketCheckoutItem checkoutItem = new TicketCheckoutItem();
            checkoutItem.setTicketType(ticketType);
            checkoutItem.setQuantity(item.getQuantity());
            checkoutItem.setUnitPriceMinor(unitPriceMinor);
            checkoutItem.setSubtotalMinor(lineTotalMinor);
            checkoutItem.setCurrency(currency);
            checkoutItems.add(checkoutItem);
        }

        TicketPromotion appliedPromotion = resolvePromotion(eventId, request.getPromotionCode(), checkoutItems);
        int taxRateBps = resolveTaxRateBps(event);

        TicketCheckout checkout = new TicketCheckout();
        checkout.setEvent(event);
        checkout.setCurrency(currency);
        checkout.setPurchaser(purchaser);
        repriceCheckout(checkout, checkoutItems, ticketTypes, activeTiers, appliedPromotion, taxRateBps);
        checkout.setStatus(checkout.getTotalMinor() != null && checkout.getTotalMinor() > 0
            ? TicketCheckoutStatus.PENDING_PAYMENT
            : TicketCheckoutStatus.COMPLETED);
        checkout.setExpiresAt(checkout.getStatus() == TicketCheckoutStatus.PENDING_PAYMENT
            ? LocalDateTime.now().plusMinutes(CHECKOUT_HOLD_MINUTES)
            : null);

        // Reserve tickets without issuing free ones yet
        List<Ticket> tickets = ticketService.issueTickets(issueRequests, principal, false);

        TicketCheckout savedCheckout = checkoutRepository.save(checkout);
        TicketCheckout checkoutForLinks = savedCheckout;

        // Link tickets to checkout
        tickets.forEach(ticket -> ticket.setCheckout(checkoutForLinks));
        tickets = ticketRepository.saveAll(tickets);

        // Persist line items
        checkoutItems.forEach(item -> item.setCheckout(checkoutForLinks));
        checkoutItemRepository.saveAll(checkoutItems);

        // Auto-complete free/zero-dollar orders
        if (savedCheckout.getStatus() == TicketCheckoutStatus.COMPLETED) {
            savedCheckout = finalizeCheckout(savedCheckout, tickets, principal);
        }

        return buildResponse(savedCheckout.getId(), checkoutItems, tickets);
    }

    /**
     * Get a checkout by ID (auto-expire if necessary).
     */
    public TicketCheckoutResponse getCheckout(UUID checkoutId, UUID eventId) {
        TicketCheckout checkout = checkoutRepository.findWithItemsById(checkoutId)
            .orElseThrow(() -> new ResourceNotFoundException("Checkout not found: " + checkoutId));
        validateCheckoutEvent(checkout, eventId);

        List<TicketCheckoutItem> items = checkout.getItems() != null ? checkout.getItems() : List.of();
        List<Ticket> tickets = ticketRepository.findByCheckoutId(checkoutId);

        if (shouldExpire(checkout)) {
            // Switch to write transaction to expire and release reservations
            return expireAndRespond(checkout, items, tickets);
        }

        return TicketCheckoutResponse.from(checkout, items, mapTickets(tickets));
    }

    /**
     * Confirm checkout (simulates payment success).
     * Intended for future PSP webhook/internal use; public endpoint removed.
     */
    public TicketCheckoutResponse confirmCheckout(UUID checkoutId, UUID eventId, UserPrincipal principal) {
        TicketCheckout checkout = checkoutRepository.findWithItemsById(checkoutId)
            .orElseThrow(() -> new ResourceNotFoundException("Checkout not found: " + checkoutId));
        validateCheckoutEvent(checkout, eventId);

        List<TicketCheckoutItem> items = checkout.getItems() != null ? checkout.getItems() : List.of();
        List<Ticket> tickets = ticketRepository.findByCheckoutId(checkoutId);
        Map<UUID, TicketType> ticketTypes = loadTicketTypesForCheckoutItems(items, eventId);
        TicketPromotion appliedPromotion = resolvePromotion(eventId, checkout.getAppliedPromotionCode(), items);
        int taxRateBps = resolveTaxRateBps(checkout.getEvent());
        Map<UUID, List<TicketPriceTier>> activeTiers = loadActivePriceTiers(ticketTypes.values());
        repriceCheckout(checkout, items, ticketTypes, activeTiers, appliedPromotion, taxRateBps);
        checkoutItemRepository.saveAll(items);
        checkoutRepository.save(checkout);

        if (checkout.getStatus() == TicketCheckoutStatus.COMPLETED) {
            return TicketCheckoutResponse.from(checkout, items, mapTickets(tickets));
        }
        if (checkout.getStatus() == TicketCheckoutStatus.CANCELLED) {
            throw new ApiException(ErrorCode.CHECKOUT_CANCELLED, "Checkout has been cancelled");
        }
        if (checkout.getStatus() == TicketCheckoutStatus.EXPIRED || shouldExpire(checkout)) {
            return expireAndRespond(checkout, items, tickets);
        }

        TicketCheckout finalized = finalizeCheckout(checkout, tickets, principal);
        return TicketCheckoutResponse.from(finalized, items, mapTickets(tickets));
    }

    /**
     * Cancel checkout and release reservations.
     */
    public TicketCheckoutResponse cancelCheckout(UUID checkoutId, UUID eventId) {
        TicketCheckout checkout = checkoutRepository.findWithItemsById(checkoutId)
            .orElseThrow(() -> new ResourceNotFoundException("Checkout not found: " + checkoutId));
        validateCheckoutEvent(checkout, eventId);

        List<TicketCheckoutItem> items = checkout.getItems() != null ? checkout.getItems() : List.of();
        List<Ticket> tickets = ticketRepository.findByCheckoutId(checkoutId);

        if (checkout.getStatus() == TicketCheckoutStatus.COMPLETED) {
            throw new ApiException(ErrorCode.CHECKOUT_COMPLETED, "Checkout is already completed and cannot be cancelled");
        }
        if (checkout.getStatus() == TicketCheckoutStatus.CANCELLED) {
            return TicketCheckoutResponse.from(checkout, items, mapTickets(tickets));
        }

        releaseReservations(tickets, "Checkout cancelled");
        checkout.setStatus(TicketCheckoutStatus.CANCELLED);
        checkout.setExpiresAt(null);
        checkoutRepository.save(checkout);

        return TicketCheckoutResponse.from(checkout, items, mapTickets(tickets));
    }

    private TicketCheckoutResponse expireAndRespond(TicketCheckout checkout, List<TicketCheckoutItem> items, List<Ticket> tickets) {
        if (checkout.getStatus() != TicketCheckoutStatus.EXPIRED) {
            releaseReservations(tickets, "Checkout expired");
            checkout.setStatus(TicketCheckoutStatus.EXPIRED);
            checkout.setExpiresAt(null);
            checkoutRepository.save(checkout);
        }
        return TicketCheckoutResponse.from(checkout, items, mapTickets(tickets));
    }

    /**
     * Start payment session (placeholder until Stripe/PSP integration).
     */
    public TicketPaymentInitResponse startPayment(UUID checkoutId, UUID eventId, UserPrincipal principal) {
        TicketCheckout checkout = checkoutRepository.findWithItemsById(checkoutId)
            .orElseThrow(() -> new ResourceNotFoundException("Checkout not found: " + checkoutId));
        validateCheckoutEvent(checkout, eventId);

        if (checkout.getStatus() == TicketCheckoutStatus.CANCELLED || checkout.getStatus() == TicketCheckoutStatus.EXPIRED) {
            throw new ApiException(ErrorCode.CHECKOUT_INACTIVE, "Checkout is not active");
        }
        if (checkout.getStatus() == TicketCheckoutStatus.COMPLETED) {
            throw new ApiException(ErrorCode.CHECKOUT_COMPLETED, "Checkout already completed");
        }

        // Reprice to ensure totals are current before sending to PSP
        List<TicketCheckoutItem> items = checkout.getItems() != null ? checkout.getItems() : List.of();
        Map<UUID, TicketType> ticketTypes = loadTicketTypesForCheckoutItems(items, eventId);
        TicketPromotion appliedPromotion = resolvePromotion(eventId, checkout.getAppliedPromotionCode(), items);
        int taxRateBps = resolveTaxRateBps(checkout.getEvent());
        Map<UUID, List<TicketPriceTier>> activeTiers = loadActivePriceTiers(ticketTypes.values());
        repriceCheckout(checkout, items, ticketTypes, activeTiers, appliedPromotion, taxRateBps);
        checkoutRepository.save(checkout);
        checkoutItemRepository.saveAll(items);

        // TODO: Replace with real Stripe/PSP session creation and return client secret or hosted link.
        String fakePaymentUrl = "https://payments.example.test/checkout/" + checkout.getId();

        return TicketPaymentInitResponse.builder()
            .checkoutId(checkout.getId())
            .paymentUrl(fakePaymentUrl)
            .message("Placeholder payment link. Replace with Stripe/PSP integration.")
            .build();
    }

    private TicketCheckout finalizeCheckout(TicketCheckout checkout, List<Ticket> tickets, UserPrincipal principal) {
        if (checkout.getStatus() == TicketCheckoutStatus.COMPLETED) {
            return checkout;
        }

        if (shouldExpire(checkout)) {
            throw new ApiException(ErrorCode.CHECKOUT_EXPIRED, "Checkout session expired");
        }

        UserAccount issuedBy = principal != null && principal.getId() != null
            ? userAccountRepository.findById(principal.getId()).orElse(null)
            : null;

        Map<UUID, Integer> quantitiesByType = new HashMap<>();
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.CANCELLED || ticket.getStatus() == TicketStatus.REFUNDED) {
                continue;
            }
            if (ticket.getStatus() == TicketStatus.PENDING) {
                if (ticket.isPendingExpired()) {
                    throw new ApiException(ErrorCode.CHECKOUT_EXPIRED, "Checkout session expired");
                }
                ticket.issue(issuedBy);
            }
            if (ticket.getTicketType() != null) {
                quantitiesByType.merge(ticket.getTicketType().getId(), 1, (a, b) -> a + b);
            }
        }

        ticketRepository.saveAll(tickets);
        quantitiesByType.forEach((ticketTypeId, qty) -> ticketTypeService.confirmSale(ticketTypeId, qty));

        checkout.setStatus(TicketCheckoutStatus.COMPLETED);
        checkout.setCompletedAt(LocalDateTime.now());
        checkout.setExpiresAt(null);
        return checkoutRepository.save(checkout);
    }

    private boolean shouldExpire(TicketCheckout checkout) {
        return checkout.getStatus() == TicketCheckoutStatus.PENDING_PAYMENT &&
               checkout.getExpiresAt() != null &&
               LocalDateTime.now().isAfter(checkout.getExpiresAt());
    }

    private void releaseReservations(List<Ticket> tickets, String reason) {
        Map<UUID, Integer> quantitiesByType = new HashMap<>();
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == TicketStatus.PENDING) {
                if (ticket.getTicketType() != null) {
                    quantitiesByType.merge(ticket.getTicketType().getId(), 1, (a, b) -> a + b);
                }
                ticket.cancel(reason);
            }
        }
        if (!tickets.isEmpty()) {
            ticketRepository.saveAll(tickets);
        }
        quantitiesByType.forEach((ticketTypeId, qty) -> ticketTypeService.releaseReservation(ticketTypeId, qty));
    }

    private TicketCheckoutResponse buildResponse(UUID checkoutId, List<TicketCheckoutItem> items, List<Ticket> tickets) {
        TicketCheckout checkout = checkoutRepository.findWithItemsById(checkoutId)
            .orElseThrow(() -> new ResourceNotFoundException("Checkout not found: " + checkoutId));
        return TicketCheckoutResponse.from(checkout, items, mapTickets(tickets));
    }

    private Map<UUID, TicketType> loadTicketTypes(List<TicketCheckoutItemRequest> items, UUID eventId) {
        Set<UUID> ticketTypeIds = items.stream()
            .map(TicketCheckoutItemRequest::getTicketTypeId)
            .collect(Collectors.toSet());

        List<TicketType> types = ticketTypeRepository.findAllById(ticketTypeIds);
        if (types.size() != ticketTypeIds.size()) {
            throw new ResourceNotFoundException("One or more ticket types were not found");
        }

        Map<UUID, TicketCheckoutItemRequest> quantities = items.stream()
            .collect(Collectors.toMap(TicketCheckoutItemRequest::getTicketTypeId, i -> i, (a, b) -> a));

        for (TicketType type : types) {
            if (type.getEvent() == null || !type.getEvent().getId().equals(eventId)) {
                throw new IllegalArgumentException("Ticket type " + type.getId() + " does not belong to event " + eventId);
            }
            TicketCheckoutItemRequest item = quantities.get(type.getId());
            if (!type.canPurchase(item.getQuantity())) {
                throw new ApiException(ErrorCode.TICKET_TYPE_SOLD_OUT, "Not enough availability");
            }
        }

        return types.stream().collect(Collectors.toMap(TicketType::getId, t -> t));
    }

    private Map<UUID, TicketType> loadTicketTypesForCheckoutItems(List<TicketCheckoutItem> items, UUID eventId) {
        Set<UUID> ticketTypeIds = items.stream()
            .map(item -> item.getTicketType() != null ? item.getTicketType().getId() : null)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (ticketTypeIds.isEmpty()) {
            return Map.of();
        }

        List<TicketType> types = ticketTypeRepository.findAllById(ticketTypeIds);
        if (types.size() != ticketTypeIds.size()) {
            throw new ResourceNotFoundException("One or more ticket types were not found for checkout items");
        }

        for (TicketType type : types) {
            if (eventId != null && (type.getEvent() == null || !type.getEvent().getId().equals(eventId))) {
                throw new IllegalArgumentException("Ticket type " + type.getId() + " does not belong to event " + eventId);
            }
        }

        return types.stream().collect(Collectors.toMap(TicketType::getId, t -> t));
    }

    private void repriceCheckout(TicketCheckout checkout, List<TicketCheckoutItem> items, Map<UUID, TicketType> ticketTypes,
                                 Map<UUID, List<TicketPriceTier>> activeTiers, TicketPromotion promotion, int taxRateBps) {
        long subtotalMinor = 0L;
        long groupDiscountMinor = 0L;
        for (TicketCheckoutItem item : items) {
            UUID ticketTypeId = item.getTicketType() != null ? item.getTicketType().getId() : null;
            TicketType ticketType = ticketTypes.get(ticketTypeId);
            if (ticketType == null) {
                throw new IllegalArgumentException("Ticket type not found for checkout item");
            }
            long unitPriceMinor = resolveUnitPrice(ticketType, activeTiers != null ? activeTiers.get(ticketTypeId) : null);
            long lineTotalMinor = Math.multiplyExact(unitPriceMinor, item.getQuantity());

            long lineDiscount = resolveGroupDiscount(ticketType, item.getQuantity(), lineTotalMinor);
            if (lineDiscount > 0) {
                groupDiscountMinor = Math.addExact(groupDiscountMinor, lineDiscount);
                lineTotalMinor = Math.max(0, lineTotalMinor - lineDiscount);
            }

            item.setUnitPriceMinor(unitPriceMinor);
            item.setSubtotalMinor(lineTotalMinor);
            item.setCurrency(checkout.getCurrency());
            subtotalMinor = Math.addExact(subtotalMinor, lineTotalMinor);
        }

        checkout.setSubtotalMinor(subtotalMinor);
        checkout.setFeesMinor(0L);
        long taxMinor = taxRateBps > 0 ? (subtotalMinor * taxRateBps) / 10_000 : 0L;
        checkout.setTaxMinor(taxMinor);
        long discountMinor = 0L;
        if (promotion != null) {
            long percentDiscount = 0L;
            if (promotion.getPercentOffBasisPoints() != null && promotion.getPercentOffBasisPoints() > 0) {
                percentDiscount = (subtotalMinor * promotion.getPercentOffBasisPoints()) / 10_000;
            }
            long amountDiscount = promotion.getAmountOffMinor() != null ? promotion.getAmountOffMinor() : 0L;
            discountMinor = Math.min(subtotalMinor, percentDiscount + amountDiscount);
            checkout.setAppliedPromotionCode(promotion.getCode());
            checkout.setAppliedDiscountMinor(discountMinor);
        } else {
            checkout.setAppliedPromotionCode(null);
            checkout.setAppliedDiscountMinor(0L);
        }
        long totalDiscountMinor = Math.addExact(discountMinor, groupDiscountMinor);
        checkout.setDiscountMinor(totalDiscountMinor);
        long totalMinor = subtotalMinor + checkout.getFeesMinor() + checkout.getTaxMinor() - totalDiscountMinor;
        checkout.setTotalMinor(totalMinor);
    }

    private String determineCurrency(Iterable<TicketType> types) {
        String currency = null;
        for (TicketType type : types) {
            String typeCurrency = type.getCurrency();
            if (currency == null) {
                currency = typeCurrency;
            } else if (typeCurrency != null && !currency.equalsIgnoreCase(typeCurrency)) {
                throw new IllegalArgumentException("All ticket types in a checkout must share the same currency");
            }
        }
        return currency != null ? currency : "USD";
    }

    private Map<UUID, List<TicketPriceTier>> loadActivePriceTiers(Iterable<TicketType> types) {
        List<UUID> typeIds = new ArrayList<>();
        for (TicketType type : types) {
            if (type != null && type.getId() != null) {
                typeIds.add(type.getId());
            }
        }
        if (typeIds.isEmpty()) {
            return Map.of();
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<TicketPriceTier> tiers = ticketPriceTierRepository.findActiveByTicketTypeIds(typeIds, now);
        return tiers.stream()
            .collect(Collectors.groupingBy(t -> t.getTicketType().getId()));
    }

    private long resolveUnitPrice(TicketType ticketType, List<TicketPriceTier> activeTiers) {
        if (activeTiers != null && !activeTiers.isEmpty()) {
            TicketPriceTier tier = activeTiers.get(0);
            return tier.getPriceMinor() != null ? tier.getPriceMinor() : 0L;
        }
        if (ticketType.getEarlyBirdPriceMinor() != null && ticketType.getEarlyBirdEndDate() != null) {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            if (!now.isAfter(ticketType.getEarlyBirdEndDate())) {
                return ticketType.getEarlyBirdPriceMinor();
            }
        }
        return ticketType.getPriceMinor() != null ? ticketType.getPriceMinor() : 0L;
    }

    private long resolveGroupDiscount(TicketType ticketType, int quantity, long lineTotalMinor) {
        if (ticketType == null) {
            return 0L;
        }
        Integer minQty = ticketType.getGroupDiscountMinQuantity();
        Integer percentBps = ticketType.getGroupDiscountPercentBps();
        if (minQty == null || percentBps == null || minQty <= 0 || percentBps <= 0) {
            return 0L;
        }
        if (quantity < minQty) {
            return 0L;
        }
        long discount = (lineTotalMinor * percentBps) / 10_000;
        return Math.min(lineTotalMinor, Math.max(0L, discount));
    }

    private List<TicketCheckoutItemRequest> normalizeItems(List<TicketCheckoutItemRequest> items) {
        Map<UUID, Integer> aggregated = new LinkedHashMap<>();
        for (TicketCheckoutItemRequest item : items) {
            int newQuantity = aggregated.getOrDefault(item.getTicketTypeId(), 0) + item.getQuantity();
            if (newQuantity > 50) {
                throw new IllegalArgumentException("Quantity for ticket type " + item.getTicketTypeId() + " exceeds maximum of 50");
            }
            aggregated.put(item.getTicketTypeId(), newQuantity);
        }
        return aggregated.entrySet().stream()
            .map(entry -> new TicketCheckoutItemRequest(entry.getKey(), entry.getValue()))
            .toList();
    }

    private void validateDependencies(Event event, UserAccount purchaser, List<TicketCheckoutItemRequest> items,
                                      Map<UUID, TicketType> ticketTypes) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<UUID, Integer> quantitiesByType = new HashMap<>();
        for (TicketCheckoutItemRequest item : items) {
            quantitiesByType.merge(item.getTicketTypeId(), item.getQuantity(), Integer::sum);
        }
        List<TicketTypeDependency> dependencies = ticketTypeDependencyRepository
            .findByTicketTypeIdIn(new ArrayList<>(quantitiesByType.keySet()));
        if (dependencies.isEmpty()) {
            return;
        }

        UUID purchaserId = purchaser != null ? purchaser.getId() : null;
        String purchaserEmail = purchaser != null ? purchaser.getEmail() : null;

        for (TicketTypeDependency dependency : dependencies) {
            if (dependency.getTicketType() == null || dependency.getRequiredTicketType() == null) {
                continue;
            }
            UUID dependentTypeId = dependency.getTicketType().getId();
            UUID requiredTypeId = dependency.getRequiredTicketType().getId();
            int dependentQty = quantitiesByType.getOrDefault(dependentTypeId, 0);
            if (dependentQty <= 0) {
                continue;
            }

            int minQty = dependency.getMinQuantity() != null ? dependency.getMinQuantity() : 1;
            int requiredQty = Math.max(1, minQty) * dependentQty;

            int providedQty = quantitiesByType.getOrDefault(requiredTypeId, 0);
            if (providedQty >= requiredQty) {
                continue;
            }

            long ownedQty = 0;
            if (purchaserId != null) {
                ownedQty = ticketRepository.countValidTicketsByUserIdAndTicketTypeId(event.getId(), purchaserId, requiredTypeId);
            } else if (purchaserEmail != null) {
                ownedQty = ticketRepository.countValidTicketsByEmailAndTicketTypeId(event.getId(), purchaserEmail, requiredTypeId);
            }

            if (providedQty + ownedQty < requiredQty) {
                TicketType dependentType = dependency.getTicketType();
                TicketType requiredType = dependency.getRequiredTicketType();
                String dependentName = dependentType != null ? dependentType.getName() : dependentTypeId.toString();
                String requiredName = requiredType != null ? requiredType.getName() : requiredTypeId.toString();
                throw new IllegalArgumentException("Ticket type " + dependentName + " requires " +
                    requiredQty + " of " + requiredName);
            }
        }
    }

    private List<TicketResponse> mapTickets(List<Ticket> tickets) {
        return tickets.stream()
            .map(TicketResponse::from)
            .collect(Collectors.toList());
    }

    private UserAccount loadPurchaser(UserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        UUID userId = principal.getId();
        if (userId != null) {
            return userAccountRepository.findById(userId).orElse(principal.getUser());
        }
        return principal.getUser();
    }


    private int resolveTaxRateBps(Event event) {
        if (event == null || event.getVenue() == null) {
            return 0;
        }
        var venue = event.getVenue();
        if (venue.getCity() == null || venue.getCountry() == null) {
            return 0;
        }
        var location = locationRepository
            .findFirstByCityIgnoreCaseAndStateIgnoreCaseAndCountryIgnoreCase(
                venue.getCity(), venue.getState(), venue.getCountry())
            .or(() -> locationRepository.findFirstByCityIgnoreCaseAndCountryIgnoreCase(
                venue.getCity(), venue.getCountry()))
            .orElse(null);
        if (location == null) {
            return 0;
        }
        Integer hst = location.getHstRateBps();
        if (hst != null && hst > 0) {
            return hst;
        }
        int gst = location.getGstRateBps() != null ? location.getGstRateBps() : 0;
        int pst = location.getPstRateBps() != null ? location.getPstRateBps() : 0;
        int sales = location.getSalesTaxRateBps() != null ? location.getSalesTaxRateBps() : 0;
        int vat = location.getVatRateBps() != null ? location.getVatRateBps() : 0;
        if (gst + pst > 0) {
            return gst + pst;
        }
        if (sales > 0) {
            return sales;
        }
        if (vat > 0) {
            return vat;
        }
        return 0;
    }

    private void validateCheckoutEvent(TicketCheckout checkout, UUID eventId) {
        if (eventId == null) {
            return;
        }
        UUID checkoutEventId = checkout.getEvent() != null ? checkout.getEvent().getId() : null;
        if (checkoutEventId != null && !checkoutEventId.equals(eventId)) {
            throw new IllegalArgumentException("Checkout does not belong to event " + eventId);
        }
    }

    private TicketPromotion resolvePromotion(UUID eventId, String code, List<TicketCheckoutItem> items) {
        if (code == null || code.isBlank()) {
            return null;
        }
        UUID ticketTypeId = null;
        if (items != null && !items.isEmpty()) {
            ticketTypeId = items.get(0).getTicketType() != null ? items.get(0).getTicketType().getId() : null;
            // Ensure all items match the same ticket type when a code is used
            for (TicketCheckoutItem item : items) {
                UUID tid = item.getTicketType() != null ? item.getTicketType().getId() : null;
                if (ticketTypeId == null || tid == null || !ticketTypeId.equals(tid)) {
                    throw new IllegalArgumentException("Promotion code can only be applied when all items are the same ticket type");
                }
            }
        }
        return ticketPromotionRepository.findByEventIdAndTicketTypeIdAndCodeIgnoreCase(eventId, ticketTypeId, code)
            .filter(TicketPromotion::isValidNow)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive promotion code"));
    }
}
