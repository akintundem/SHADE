package eventplanner.common.communication.services.channel.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for Resend email requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    @NotNull(message = "Recipients are required")
    private List<String> to;

    @Email(message = "From email must be valid")
    @NotBlank(message = "From email is required")
    private String from;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String html; // HTML content (optional if using template)

    private String text; // Plain text content (optional if using template)

    private List<EmailAttachment> attachments;

    private String templateId;

    private Map<String, Object> templateVariables;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailAttachment {
        @NotBlank(message = "Content is required")
        private String content; // Base64 encoded

        @NotBlank(message = "Filename is required")
        private String filename;

        private String type;
    }
}

