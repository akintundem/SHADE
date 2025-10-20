package ai.eventplanner.assistant.service;

import ai.eventplanner.assistant.dto.ShadeConversationRequest;
import ai.eventplanner.assistant.dto.ShadeConversationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Thin client that delegates Shade conversations to the LangChain-powered Python service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
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
                        log.error("Shade assistant service error", throwable);
                        return Mono.just(fallbackResponse(request.getSessionId()));
                    })
                    .block();
        } catch (Exception ex) {
            log.error("Failed to reach Shade assistant service", ex);
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
