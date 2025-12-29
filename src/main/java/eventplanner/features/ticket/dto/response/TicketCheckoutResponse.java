package eventplanner.features.ticket.dto.response;

import eventplanner.features.ticket.entity.TicketCheckout;
import eventplanner.features.ticket.entity.TicketCheckoutItem;
import eventplanner.features.ticket.enums.TicketCheckoutStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response payload for ticket checkout session.
 */
@Data
@Builder
public class TicketCheckoutResponse {
    private UUID id;
    private UUID eventId;
    private String eventName;
    private TicketCheckoutStatus status;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private TicketCostBreakdown cost;
    private List<TicketCheckoutItemResponse> items;
    private List<TicketResponse> tickets;

    public static TicketCheckoutResponse from(TicketCheckout checkout, List<TicketCheckoutItem> items, List<TicketResponse> tickets) {
        TicketCostBreakdown cost = TicketCostBreakdown.builder()
            .currency(checkout.getCurrency())
            .subtotalMinor(checkout.getSubtotalMinor())
            .feesMinor(checkout.getFeesMinor())
            .taxMinor(checkout.getTaxMinor())
            .discountMinor(checkout.getDiscountMinor())
            .totalMinor(checkout.getTotalMinor())
            .build();

        List<TicketCheckoutItemResponse> itemResponses = items.stream()
            .map(TicketCheckoutItemResponse::from)
            .collect(Collectors.toList());

        return TicketCheckoutResponse.builder()
            .id(checkout.getId())
            .eventId(checkout.getEvent() != null ? checkout.getEvent().getId() : null)
            .eventName(checkout.getEvent() != null ? checkout.getEvent().getName() : null)
            .status(checkout.getStatus())
            .completedAt(checkout.getCompletedAt())
            .createdAt(checkout.getCreatedAt())
            .updatedAt(checkout.getUpdatedAt())
            .cost(cost)
            .items(itemResponses)
            .tickets(tickets)
            .build();
    }
}
