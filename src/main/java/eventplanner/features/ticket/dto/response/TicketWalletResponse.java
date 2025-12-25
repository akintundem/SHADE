package eventplanner.features.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wallet-ready details for adding tickets to Apple/Google Wallet.
 * Retrieved via GET /api/v1/tickets/{id}/wallet-pass endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketWalletResponse {

    private Boolean available;
    private String ticketNumber;
    private String ticketTypeName;
    private String eventName;
    private LocalDateTime eventStartDateTime;
    private LocalDateTime eventEndDateTime;
    private String venueAddress;
    private String venueCity;
    private String venueState;
    private String venueCountry;
    private String barcodeMessage;
}
