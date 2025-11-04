package eventplanner.security.auth.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogoutResponse {
    private String message;
    private boolean success;
}
