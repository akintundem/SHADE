package eventplanner.features.attendee.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkAttendanceRequest {
    
    @NotNull(message = "Event ID is required")
    private UUID eventId;
    
    @NotEmpty(message = "Attendees list cannot be empty")
    @Valid
    private List<CreateAttendanceRequest> attendees;
}
