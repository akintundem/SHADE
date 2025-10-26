package ai.eventplanner.auth.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PasswordResponse {
    private String message;
    private boolean success;
}
