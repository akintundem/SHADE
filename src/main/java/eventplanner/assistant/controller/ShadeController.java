package eventplanner.assistant.controller;

import eventplanner.assistant.dto.ShadeConversationRequest;
import eventplanner.assistant.dto.ShadeConversationResponse;
import eventplanner.assistant.service.ShadeConversationService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simplified controller for Shade AI assistant - single conversational endpoint
 * Authorization handled by security filters
 */
@RestController
@RequestMapping("/api/v1/assistant/shade")
@RequiredArgsConstructor
public class ShadeController {

    private final ShadeConversationService shadeConversationService;

    /**
     * Single conversational endpoint for all Shade interactions
     * Handles event creation, questions about capabilities, event types, etc.
     */
    @PostMapping("/chat")
    @RequiresPermission(value = RbacPermissions.AUTH_ME, resources = {"user_id=#principal.id"})
    public ResponseEntity<ShadeConversationResponse> chat(@Valid @RequestBody ShadeConversationRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        ShadeConversationResponse response = shadeConversationService.converse(request);
        return ResponseEntity.ok(response);
    }
}
