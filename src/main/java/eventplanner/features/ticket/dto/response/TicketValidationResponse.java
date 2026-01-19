package eventplanner.features.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for ticket validation result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketValidationResponse {

    /**
     * Whether the ticket is valid and was successfully validated.
     */
    private Boolean valid;

    /**
     * Ticket information if validation was successful.
     */
    private TicketResponse ticket;

    /**
     * Human-readable message about the validation result.
     */
    private String message;

    /**
     * Error code if validation failed.
     * Possible values: INVALID_CODE, ALREADY_VALIDATED, CANCELLED, EXPIRED, etc.
     */
    private String errorCode;
}

