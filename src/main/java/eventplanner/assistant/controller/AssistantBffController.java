package eventplanner.assistant.controller;

import eventplanner.assistant.dto.ChatRequest;
import eventplanner.assistant.dto.AssistantChatResponse;
import eventplanner.assistant.service.ShadeAssistantService;
import eventplanner.security.auth.service.UserPrincipal;
import eventplanner.security.authorization.rbac.RbacPermissions;
import eventplanner.security.authorization.rbac.annotation.RequiresPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantBffController {

    private final ShadeAssistantService shadeAssistantService;

    @PostMapping("/chat")
    @RequiresPermission(value = RbacPermissions.AUTH_ME, resources = {"user_id=#principal.id"})
    public ResponseEntity<AssistantChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = requireUserId(principal);

        AssistantChatResponse response = shadeAssistantService.sendMessage(
                request.getMessage(),
                userId.toString(),
                request.getChatId(),
                request.getEventId(),
                correlationId
        );
        return ResponseEntity.ok(response);
    }

    private UUID requireUserId(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal.getId();
    }
}

