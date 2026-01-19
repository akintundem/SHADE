package eventplanner.features.ticket.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketApprovalDecisionRequest {

    private String note;
    private Boolean sendEmail = true;
    private Boolean sendPush = true;
}
