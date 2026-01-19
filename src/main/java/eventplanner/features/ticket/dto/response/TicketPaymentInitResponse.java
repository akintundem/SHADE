package eventplanner.features.ticket.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Placeholder response for starting a payment session.
 */
@Data
@Builder
public class TicketPaymentInitResponse {
    private UUID checkoutId;
    private String paymentUrl;
    private String message;
}
