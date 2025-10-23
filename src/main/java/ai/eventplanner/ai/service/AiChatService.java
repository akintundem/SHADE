package ai.eventplanner.ai.service;

import ai.eventplanner.ai.dto.AiChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for AI chat functionality
 * Forwards requests to the Python Shade assistant service
 */
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final RestTemplate restTemplate;
    private final String shadeAssistantUrl = "http://localhost:8000";

    public AiChatResponse sendMessage(String message, String userId, String chatId, String eventId) {
        String url = shadeAssistantUrl + "/chat";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", message);
        requestBody.put("user_id", userId);
        if (chatId != null) {
            requestBody.put("chat_id", chatId);
        }
        if (eventId != null) {
            requestBody.put("event_id", eventId);
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Internal service auth header
        String internalSecret = System.getenv().getOrDefault("INTERNAL_ASSISTANT_SECRET", "dev-internal-secret");
        headers.add("X-Internal-Service-Auth", internalSecret);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> responseBody = response.getBody();
            
            return AiChatResponse.builder()
                    .reply((String) responseBody.get("reply"))
                    .toolUsed((String) responseBody.get("tool_used"))
                    .data(responseBody.get("data"))
                    .showChips((Boolean) responseBody.get("show_chips"))
                    .chatId((String) responseBody.get("chat_id"))
                    .userId((String) responseBody.get("user_id"))
                    .eventId((String) responseBody.get("event_id"))
                    .ui(responseBody.get("ui"))
                    .structuredResponse(responseBody.get("structured_response"))
                    .build();
        } catch (Exception e) {
            return AiChatResponse.builder()
                    .reply("I'm having trouble connecting to the AI assistant right now. Please try again shortly.")
                    .toolUsed("error")
                    .data(Map.<String, Object>of("error", e.getMessage()))
                    .showChips(false)
                    .chatId(chatId)
                    .userId(userId)
                    .eventId(eventId)
                    .build();
        }
    }
}
