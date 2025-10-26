package ai.eventplanner.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionResponse {
    private String message;
    private boolean success;
}
