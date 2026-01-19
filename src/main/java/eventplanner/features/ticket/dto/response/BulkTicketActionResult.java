package eventplanner.features.ticket.dto.response;

import eventplanner.features.ticket.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTicketActionResult {

    private UUID ticketId;
    private boolean success;
    private String message;
    private TicketStatus status;
}
