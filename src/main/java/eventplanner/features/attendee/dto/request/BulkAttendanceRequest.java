package eventplanner.features.attendee.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkAttendanceRequest {
    
    // eventId is injected from @PathVariable in the controller, so it can be null in the request body
    private UUID eventId;
    
    @NotEmpty(message = "Attendees list cannot be empty")
    @Valid
    @JsonAlias({"attendances"})
    private List<CreateAttendanceRequest> attendees;
}
