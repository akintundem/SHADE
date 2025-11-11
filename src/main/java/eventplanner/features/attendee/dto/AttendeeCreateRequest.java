package eventplanner.features.attendee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * @deprecated Use {@link BulkAttendeeCreateRequest} instead for bulk operations.
 * This DTO is inefficient as it requires repeating eventId for each attendee.
 * Kept for backward compatibility only.
 */
@Deprecated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendeeCreateRequest {

    @NotNull
    private UUID eventId;

    @NotBlank
    private String name;

    private String email;

    private String phone;
}

