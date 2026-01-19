package eventplanner.common.communication.services.channel.push.dto;

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
public class PushJobRequest {
    private List<String> to;
    private String title;
    private String body;
    private Map<String, String> data;
}
