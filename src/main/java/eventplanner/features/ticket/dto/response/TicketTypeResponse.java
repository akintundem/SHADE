package eventplanner.features.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import eventplanner.features.ticket.entity.TicketType;
import eventplanner.features.ticket.enums.TicketTypeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for ticket type information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketTypeResponse {

    private UUID id;
    private UUID eventId;
    private String name;
    private TicketTypeCategory category;
    private String description;
    private Long priceMinor; // NULL for free tickets
    private String currency; // ISO 4217 currency code (e.g., "USD", "EUR", "GBP") - validated using JavaMoney (JSR 354)
    private Integer quantityAvailable;
    private Integer quantitySold;
    private Integer quantityReserved;
    private Integer quantityRemaining; // Computed: available - sold - reserved
    private Boolean isActive;
    private LocalDateTime saleStartDate;
    private LocalDateTime saleEndDate;
    private Boolean isOnSale; // Computed: checks if currently on sale
    private Integer maxTicketsPerPerson;
    private Boolean requiresApproval;
    private Long earlyBirdPriceMinor;
    private LocalDateTime earlyBirdEndDate;
    private Integer groupDiscountMinQuantity;
    private Integer groupDiscountPercentBps;
    private List<TicketPriceTierResponse> priceTiers;
    private List<TicketTypeDependencyResponse> dependencies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Create a TicketTypeResponse from a TicketType entity.
     */
    public static TicketTypeResponse from(TicketType ticketType) {
        return TicketTypeResponse.builder()
                .id(ticketType.getId())
                .eventId(ticketType.getEvent() != null ? ticketType.getEvent().getId() : null)
                .name(ticketType.getName())
                .category(ticketType.getCategory())
                .description(ticketType.getDescription())
                .priceMinor(ticketType.getPriceMinor())
                .currency(ticketType.getCurrency())
                .quantityAvailable(ticketType.getQuantityAvailable())
                .quantitySold(ticketType.getQuantitySold())
                .quantityReserved(ticketType.getQuantityReserved())
                .quantityRemaining(ticketType.getQuantityRemaining())
                .isActive(ticketType.getIsActive())
                .saleStartDate(ticketType.getSaleStartDate())
                .saleEndDate(ticketType.getSaleEndDate())
                .isOnSale(ticketType.isOnSale())
                .maxTicketsPerPerson(ticketType.getMaxTicketsPerPerson())
                .requiresApproval(ticketType.getRequiresApproval())
                .earlyBirdPriceMinor(ticketType.getEarlyBirdPriceMinor())
                .earlyBirdEndDate(ticketType.getEarlyBirdEndDate())
                .groupDiscountMinQuantity(ticketType.getGroupDiscountMinQuantity())
                .groupDiscountPercentBps(ticketType.getGroupDiscountPercentBps())
                .createdAt(ticketType.getCreatedAt())
                .updatedAt(ticketType.getUpdatedAt())
                .build();
    }
}
