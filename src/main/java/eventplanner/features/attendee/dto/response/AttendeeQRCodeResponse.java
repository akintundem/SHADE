package eventplanner.features.attendee.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API response encapsulating both the raw QR code value and the rendered image.
 */
@Getter
@Setter
@Builder
@Schema(description = "Attendee QR code payload including rendered image")
public class AttendeeQRCodeResponse {

    @Schema(description = "Attendance identifier")
    private UUID attendanceId;

    @Schema(description = "Event identifier")
    private UUID eventId;

    @Schema(description = "Raw QR code value that should be encoded in the image")
    private String qrCode;

    @Schema(description = "Base64 data URI representing the QR code image")
    private String qrCodeImageBase64;

    @Schema(description = "Whether the QR code has been consumed for check-in")
    private Boolean qrCodeUsed;

    @Schema(description = "Timestamp when the QR code was last generated or updated")
    private LocalDateTime generatedAt;
}

