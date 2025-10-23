package ai.eventplanner.attendee.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportResponse {
    
    private String exportId;
    private String format;
    private String status;
    private String downloadUrl;
    private Long recordCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private List<String> fields;
    private String fileName;
}
