package eventplanner.assistant.service;

import eventplanner.assistant.dto.ShadeConversationRequest;
import eventplanner.assistant.dto.ShadeConversationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Thin client that delegates Shade conversations to the LangChain-powered Python service.
 */
@Service
@RequiredArgsConstructor
public class ShadeConversationService {

    private final WebClient shadeAssistantClient;

    public ShadeConversationResponse converse(ShadeConversationRequest request) {
        try {
            return shadeAssistantClient
                    .post()
                    .uri("/api/v1/assistant/shade/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ShadeConversationResponse.class)
                    .onErrorResume(throwable -> {
                        return Mono.just(fallbackResponse(request.getSessionId()));
                    })
                    .block();
        } catch (Exception ex) {
            return fallbackResponse(request.getSessionId());
        }
    }

    private ShadeConversationResponse fallbackResponse(java.util.UUID sessionId) {
        return ShadeConversationResponse.builder()
                .sessionId(sessionId)
                .message("I’m having trouble reaching our planning assistant right now. Please try again shortly.")
                .intent("systemIssue")
                .requiresConfirmation(false)
                .build();
    }
}
