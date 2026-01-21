package eventplanner.features.feeds.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request to create a quote post (repost with comment)")
public class QuotePostRequest {

    @Size(max = 4000, message = "Quote text must not exceed 4000 characters")
    @Schema(description = "Your comment on the repost", example = "This is amazing!")
    private String quoteText;
}
