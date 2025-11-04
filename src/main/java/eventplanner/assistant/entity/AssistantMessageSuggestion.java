package eventplanner.assistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssistantMessageSuggestion {

    @Column(name = "suggestion_category", length = 100)
    private String category;

    @Column(name = "suggestion_value", length = 255)
    private String value;
}
