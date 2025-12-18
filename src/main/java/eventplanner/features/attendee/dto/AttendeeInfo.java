package eventplanner.features.attendee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual attendee information (without eventId).
 * Used in bulk operations where eventId is specified once at the top level.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendeeInfo {

    @NotBlank(message = "Name is required")
    private String name;

    private String email;

    private String phone;
}

