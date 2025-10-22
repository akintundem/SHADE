package ai.eventplanner.sendgrid.dto;

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
 * DTO for SendGrid email requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    @NotNull(message = "Recipients are required")
    private List<EmailRecipient> to;

    @Email(message = "From email must be valid")
    @NotBlank(message = "From email is required")
    private String from;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Content is required")
    private String content;

    @Builder.Default
    private String contentType = "text/plain"; // text/plain or text/html

    private List<EmailAttachment> attachments;

    private Map<String, Object> customArgs;

    private String templateId;

    private Map<String, Object> templateData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailRecipient {
        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        private String email;

        private String name;

        private Map<String, Object> substitutions;
    }

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

        @Builder.Default
        private String disposition = "attachment";
    }
}
