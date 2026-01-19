package eventplanner.features.ticket.dto.response;

import eventplanner.features.ticket.enums.BulkTicketAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTicketActionResponse {

    private UUID eventId;
    private BulkTicketAction action;
    private int total;
    private int successCount;
    private int failureCount;
    private List<BulkTicketActionResult> results;
}
