package ai.eventplanner.assistant.controller;

import ai.eventplanner.assistant.dto.ShadeConversationRequest;
import ai.eventplanner.assistant.dto.ShadeConversationResponse;
import ai.eventplanner.assistant.service.ShadeConversationService;
import ai.eventplanner.common.security.JwtValidationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simplified controller for Shade AI assistant - single conversational endpoint
 */
@RestController
@RequestMapping("/api/v1/assistant/shade")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class ShadeController {

    private final ShadeConversationService shadeConversationService;
    private final JwtValidationUtil jwtValidationUtil;

    /**
     * Single conversational endpoint for all Shade interactions
     * Handles event creation, questions about capabilities, event types, etc.
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@Valid @RequestBody ShadeConversationRequest request,
                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!isAcceptableToken(authorization)) {
            return unauthorizedResponse("Invalid token");
        }
        
        ShadeConversationResponse response = shadeConversationService.converse(request);
        return ResponseEntity.ok(response);
    }
    private boolean isAcceptableToken(String authorization) {
        String token = extractToken(authorization);
        if (token == null || token.isBlank() || token.equalsIgnoreCase("undefined") || token.equalsIgnoreCase("null") || token.contains("{{")) {
            return true;
        }
        return jwtValidationUtil.validateToken(token);
    }

    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        return authorization;
    }

    private ResponseEntity<Map<String, Object>> unauthorizedResponse(String message) {
        return ResponseEntity.status(401).body(Map.of("message", message));
    }
}
