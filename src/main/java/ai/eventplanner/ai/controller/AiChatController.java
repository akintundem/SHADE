package ai.eventplanner.ai.controller;

import ai.eventplanner.ai.dto.AiChatRequest;
import ai.eventplanner.ai.dto.AiChatResponse;
import ai.eventplanner.ai.service.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public AI chat controller - no authentication required
 * This provides a simple interface for AI chat functionality
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        AiChatResponse response = aiChatService.sendMessage(
                request.getMessage(),
                request.getUserId(),
                request.getChatId(),
                request.getEventId()
        );
        return ResponseEntity.ok(response);
    }
}
