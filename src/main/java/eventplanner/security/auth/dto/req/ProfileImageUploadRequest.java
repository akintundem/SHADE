package eventplanner.security.auth.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileImageUploadRequest {

    @NotBlank(message = "File name is required")
    @Schema(description = "Original file name", example = "avatar.png")
    private String fileName;

    @NotBlank(message = "Content type is required")
    @Schema(description = "MIME content type", example = "image/png")
    private String contentType;
}
