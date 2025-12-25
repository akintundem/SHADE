package eventplanner.features.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import eventplanner.features.ticket.enums.TicketTypeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private BigDecimal price; // NULL for free tickets
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

