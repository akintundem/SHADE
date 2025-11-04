package eventplanner.common.communication.channels.email.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Resend email responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {

    private boolean success;

    private String messageId;

    private int statusCode;

    private String statusMessage;

    private LocalDateTime sentAt;

    private List<String> errors;
}

