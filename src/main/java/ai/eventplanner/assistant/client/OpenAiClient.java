package ai.eventplanner.assistant.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiClient {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final Environment environment;

    public boolean isConfigured() {
        return StringUtils.hasText(resolveApiKey());
    }

    public String createChatCompletion(List<Map<String, String>> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            throw new IllegalArgumentException("messages must not be empty");
        }

        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", resolveModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.4);
        requestBody.put("max_tokens", 800);

        JsonNode response = restClient.post()
                .uri("/v1/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        return extractMessageContent(response);
    }

    private String extractMessageContent(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI response was empty");
        }
        JsonNode choices = response.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("OpenAI response missing choices");
        }
        JsonNode messageNode = choices.get(0).path("message");
        String content = messageNode.path("content").asText(null);
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("OpenAI response missing message content");
        }
        return content;
    }

    private String resolveApiKey() {
        String key = environment.getProperty("OPENAI_API_KEY");
        if (!StringUtils.hasText(key)) {
            key = environment.getProperty("openai.api-key");
        }
        return StringUtils.hasText(key) ? key.trim() : null;
    }

    private String resolveModel() {
        String configured = environment.getProperty("openai.model");
        if (StringUtils.hasText(configured)) {
            return configured.trim();
        }
        return DEFAULT_MODEL;
    }
}
