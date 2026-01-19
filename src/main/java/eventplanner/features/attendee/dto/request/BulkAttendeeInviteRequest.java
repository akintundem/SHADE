package eventplanner.features.attendee.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Bulk attendee invite payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkAttendeeInviteRequest {

    @NotEmpty(message = "At least one invite is required")
    @Size(max = 100, message = "Cannot send more than 100 invites at once")
    @Valid
    private List<CreateAttendeeInviteRequest> invites;
}
