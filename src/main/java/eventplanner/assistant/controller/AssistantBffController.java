package eventplanner.assistant.controller;

import eventplanner.assistant.dto.ChatRequest;
import eventplanner.assistant.dto.AssistantChatResponse;
import eventplanner.assistant.service.ShadeAssistantService;
import eventplanner.security.auth.service.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantBffController {

    private final ShadeAssistantService shadeAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        // Extract authenticated user id from SecurityContext; ignore any userId in body
        UUID userId = getCurrentUserId().orElseThrow(() -> new RuntimeException("Unauthenticated"));

        // Forward to Python assistant with internal auth headers and correlation id
        AssistantChatResponse response = shadeAssistantService.sendMessage(
                request.getMessage(),
                userId.toString(),
                request.getChatId(),
                request.getEventId(),
                correlationId
        );
        return ResponseEntity.ok(response);
    }

    private Optional<UUID> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return Optional.empty();
        }
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return Optional.ofNullable(principal.getUser().getId());
    }
}


