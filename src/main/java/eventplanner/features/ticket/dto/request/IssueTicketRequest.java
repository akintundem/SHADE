package eventplanner.features.ticket.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for issuing one or more tickets to an attendee.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueTicketRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotNull(message = "Ticket type ID is required")
    private UUID ticketTypeId;

    /**
     * Attendee ID (optional - if not provided, email and name must be provided).
     * Use this for tickets issued to registered attendees.
     */
    private UUID attendeeId;

    /**
     * Email address of ticket owner (required if attendeeId is not provided).
     * Use this for tickets issued to users not registered on the platform.
     */
    @Email(message = "Email must be valid if provided")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String ownerEmail;

    /**
     * Name of ticket owner (required if attendeeId is not provided).
     * Use this for tickets issued to users not registered on the platform.
     */
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String ownerName;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 50, message = "Quantity must not exceed 50")
    private Integer quantity;

    /**
     * Send email notification to attendee.
     */
    private Boolean sendEmail = false;

    /**
     * Send push notification to attendee (requires user account).
     */
    private Boolean sendPushNotification = false;
}

