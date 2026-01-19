package eventplanner.features.ticket.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketWaitlistFulfillRequest {

    private Boolean sendEmail = true;
    private Boolean sendPush = true;
}
