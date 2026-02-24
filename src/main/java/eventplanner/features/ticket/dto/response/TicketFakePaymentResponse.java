package eventplanner.features.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Response for fake payment endpoint (demo mode).
 * Success vs failure is randomized in a 1:9 ratio (fail once per ~10 attempts).
 */
@Data
@Builder
public class TicketFakePaymentResponse {
    private boolean success;
    private String message;
    private TicketCheckoutResponse checkout;
}
