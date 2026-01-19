package eventplanner.features.attendee.dto.request;

import eventplanner.security.auth.enums.VisibilityLevel;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Attendee payload used inside bulk requests (eventId supplied separately).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendeeInfo {

    /**
     * Directory user ID; optional. When provided, name/email are pulled from the account.
     */
    private UUID userId;

    /**
     * Email, required when userId is absent.
     */
    @Email(message = "Valid email is required if userId is not provided")
    private String email;

    /**
     * Name when no userId is supplied; ignored otherwise.
     */
    private String name;

    /**
     * Optional per-event visibility; defaults to the user's global preference.
     */
    private VisibilityLevel participationVisibility;
}
