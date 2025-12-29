package eventplanner.features.ticket.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload to start a ticket checkout session.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketCheckoutRequest {

    @Valid
    @NotEmpty(message = "At least one ticket item is required")
    private List<TicketCheckoutItemRequest> items;
}
