package eventplanner.common.communication.services.channel.email.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailJobRequest {
    private String templateId;
    private List<String> to;
    private String from;
    private String subject;
    private Map<String, Object> variables;
}
