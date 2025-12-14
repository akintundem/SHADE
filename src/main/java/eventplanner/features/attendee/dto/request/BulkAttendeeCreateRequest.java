package eventplanner.features.attendee.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for bulk attendee creation.
 * More efficient than repeating eventId for each attendee.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkAttendeeCreateRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotEmpty(message = "At least one attendee is required")
    @Size(max = 100, message = "Cannot add more than 100 attendees at once")
    @Valid
    private List<AttendeeInfo> attendees;
}
