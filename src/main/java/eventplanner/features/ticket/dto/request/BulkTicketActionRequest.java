package eventplanner.features.ticket.dto.request;

import eventplanner.features.ticket.enums.BulkTicketAction;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkTicketActionRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotNull(message = "Action is required")
    private BulkTicketAction action;

    @NotEmpty(message = "At least one ticket ID is required")
    @Size(max = 200, message = "Cannot process more than 200 tickets at once")
    private List<UUID> ticketIds;

    private String reason;
    private Boolean sendEmail = true;
    private Boolean sendPush = true;
}
