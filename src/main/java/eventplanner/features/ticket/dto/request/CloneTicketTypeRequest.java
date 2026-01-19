package eventplanner.features.ticket.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloneTicketTypeRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    private Boolean isActive = false;
}
