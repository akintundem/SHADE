package eventplanner.features.attendee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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

