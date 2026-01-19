package eventplanner.features.event.dto.response;

import eventplanner.features.ticket.enums.TicketTypeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary of a ticket type for inclusion in event responses.
 * Provides essential information for displaying ticket options on mobile.
 */
@Schema(description = "Summary of a ticket type available for an event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketTypeSummary {

    @Schema(description = "Unique identifier of the ticket type")
    private UUID id;

    @Schema(description = "Name of the ticket type", example = "VIP Access")
    private String name;

    @Schema(description = "Category of the ticket type", example = "VIP")
    private TicketTypeCategory category;

    @Schema(description = "Description of what the ticket includes", example = "Front row seating with meet & greet")
    private String description;

    @Schema(description = "Price per ticket in smallest unit (null for free tickets)", example = "15000")
    private Long priceMinor;

    @Schema(description = "Currency code (ISO 4217)", example = "USD")
    private String currency;

    @Schema(description = "Whether this is a free ticket", example = "false")
    private Boolean isFree;

    @Schema(description = "Total quantity available for this ticket type", example = "100")
    private Integer quantityTotal;

    @Schema(description = "Quantity remaining available for purchase", example = "45")
    private Integer quantityRemaining;

    @Schema(description = "Quantity already sold", example = "50")
    private Integer quantitySold;

    @Schema(description = "Whether tickets of this type are currently available for purchase", example = "true")
    private Boolean isAvailable;

    @Schema(description = "Whether this ticket type is active", example = "true")
    private Boolean isActive;

    @Schema(description = "When ticket sales start", example = "2024-01-01T00:00:00")
    private LocalDateTime saleStartDate;

    @Schema(description = "When ticket sales end", example = "2024-06-14T23:59:59")
    private LocalDateTime saleEndDate;

    @Schema(description = "Whether the sale period is currently active", example = "true")
    private Boolean isOnSale;

    @Schema(description = "Maximum tickets a person can purchase (null = unlimited)", example = "4")
    private Integer maxPerPerson;

    @Schema(description = "Whether this ticket type requires approval after purchase", example = "false")
    private Boolean requiresApproval;

    @Schema(description = "Status message for display", example = "Only 5 left!")
    private String statusMessage;
}
