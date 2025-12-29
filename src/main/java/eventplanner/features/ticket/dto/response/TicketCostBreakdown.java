package eventplanner.features.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Cost breakdown for a checkout session.
 */
@Data
@Builder
public class TicketCostBreakdown {
    private String currency;
    private Long subtotalMinor;
    private Long feesMinor;
    private Long taxMinor;
    private Long discountMinor;
    private Long totalMinor;
}
