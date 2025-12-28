package eventplanner.features.attendee.dto.request;

import eventplanner.common.domain.enums.VisibilityLevel;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for individual attendee information (without eventId).
 * Used in bulk operations where eventId is specified once at the top level.
 * Supports adding attendees by userId (from directory) or email.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendeeInfo {

    /**
     * User ID from the directory (optional - if provided, name/email will be auto-filled from user account)
     */
    private UUID userId;

    /**
     * Email address (required if userId is not provided)
     */
    @Email(message = "Valid email is required if userId is not provided")
    private String email;

    /**
     * Name (required if userId is not provided, otherwise auto-filled from user account)
     */
    private String name;

    /**
     * Participation visibility for this specific event (optional).
     * If not provided, defaults to the user's global eventParticipationVisibility setting.
     * Allows users to override their default on a per-event basis.
     */
    private VisibilityLevel participationVisibility;
}
