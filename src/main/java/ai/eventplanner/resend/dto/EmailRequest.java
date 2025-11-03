package ai.eventplanner.resend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    private String html; // HTML content

    private String text; // Plain text content

    private List<EmailAttachment> attachments;

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

