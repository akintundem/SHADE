package eventplanner.common.communication.services.core.dto;

import eventplanner.common.domain.enums.CommunicationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for sending notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotNull(message = "Communication type is required")
    private CommunicationType type;

    @NotBlank(message = "Recipient is required")
    private String to; // Email address for EMAIL, userId (as string) for PUSH_NOTIFICATION

    @NotBlank(message = "Subject is required")
    private String subject;

    /**
     * From address for email sends (e.g., "Shade <noreply@shade.com>").
     * Required when type = EMAIL.
     */
    private String from;

    private String templateId; // Required for EMAIL, null for PUSH_NOTIFICATION

    @Builder.Default
    private Map<String, Object> templateVariables = new HashMap<>(); // Variables for EMAIL template or data for PUSH

    private UUID eventId; // Optional event ID for event-related communications
}
