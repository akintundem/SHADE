package eventplanner.features.ticket.dto.response;

import eventplanner.features.ticket.entity.TicketCheckoutItem;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Line item details in a checkout response.
 */
@Data
@Builder
public class TicketCheckoutItemResponse {
    private UUID ticketTypeId;
    private String ticketTypeName;
    private Integer quantity;
    private Long unitPriceMinor;
    private Long subtotalMinor;
    private String currency;

    public static TicketCheckoutItemResponse from(TicketCheckoutItem item) {
        return TicketCheckoutItemResponse.builder()
            .ticketTypeId(item.getTicketType() != null ? item.getTicketType().getId() : null)
            .ticketTypeName(item.getTicketType() != null ? item.getTicketType().getName() : null)
            .quantity(item.getQuantity())
            .unitPriceMinor(item.getUnitPriceMinor())
            .subtotalMinor(item.getSubtotalMinor())
            .currency(item.getCurrency())
            .build();
    }
}
