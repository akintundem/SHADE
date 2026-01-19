package eventplanner.features.attendee.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkRsvpUpdateRequest {

    @NotEmpty(message = "At least one RSVP update is required")
    @Size(max = 200, message = "Cannot update more than 200 attendees at once")
    @Valid
    private List<BulkRsvpUpdateItem> updates;

    private String note;
}
