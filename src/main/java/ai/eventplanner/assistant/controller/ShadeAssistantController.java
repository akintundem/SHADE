package ai.eventplanner.assistant.controller;

import ai.eventplanner.event.service.EventService;
import ai.eventplanner.event.dto.response.EventResponse;
import ai.eventplanner.assistant.service.ShadeAssistantService;
import ai.eventplanner.assistant.dto.ChatRequest;
import ai.eventplanner.assistant.dto.AssistantChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/assistant")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
@ConditionalOnProperty(name = "assistant.legacy.enabled", havingValue = "true", matchIfMissing = false)
public class ShadeAssistantController {

    @Autowired
    private ShadeAssistantService shadeAssistantService;
    
    @Autowired
    private EventService eventService;

    /**
     * Process chat message with Shade AI assistant
     * Handles tool calls and delegates to appropriate services
     */
    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> processMessage(@RequestBody ChatRequest request) {
        try {
            // Send message to Python AI service
            AssistantChatResponse response = shadeAssistantService.sendMessage(
                request.getMessage(), 
                request.getUserId(), 
                request.getChatId(), 
                request.getEventId(),
                null
            );
            
            // Check if tool call is required
            if (response.getData() != null && response.getData().containsKey("tool_call_required")) {
                boolean toolCallRequired = (Boolean) response.getData().get("tool_call_required");
                String toolName = (String) response.getData().get("tool_name");
                
                if (toolCallRequired) {
                    // Handle tool calls based on tool name
                    switch (toolName) {
                        case "CreateEvent":
                            handleCreateEventTool(response);
                            break;
                        case "UpdateEvent":
                            handleUpdateEventTool(response);
                            break;
                        default:
                            // Unknown tool, log and continue
                            System.out.println("Unknown tool: " + toolName);
                    }
                }
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Handle errors gracefully without exposing internal details
            AssistantChatResponse errorResponse = new AssistantChatResponse();
            errorResponse.setReply("I'm sorry, I encountered an error. Please try again.");
            errorResponse.setToolUsed("Error");
            errorResponse.setData(null);
            errorResponse.setShowChips(false);
            errorResponse.setChatId(request.getChatId());
            errorResponse.setUserId(request.getUserId());
            errorResponse.setEventId(request.getEventId());
            errorResponse.setSuccess(false);
            // Don't expose internal error details
            errorResponse.setError("Internal server error");
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Handle CreateEvent tool call
     */
    private void handleCreateEventTool(AssistantChatResponse response) {
        try {
            Map<String, Object> data = response.getData();
            @SuppressWarnings("unchecked")
            Map<String, Object> validatedData = (Map<String, Object>) data.get("validated_data");
            String chatId = response.getChatId();
            
            // Call EventService to create the event
            EventResponse eventResponse = eventService.createEvent(validatedData, chatId);
            
            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("event_created", true);
            responseData.put("event_id", eventResponse.getId());
            responseData.put("event_name", eventResponse.getName());
            responseData.put("event_type", eventResponse.getEventType());
            
            // Include venues if available
            if (data.containsKey("venues")) {
                responseData.put("venues", data.get("venues"));
            }
            
            // Update response with event details
            response.setEventId(eventResponse.getId().toString());
            response.setData(responseData);
            
            System.out.println("✅ Event created successfully: " + eventResponse.getId());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to create event: " + e.getMessage());
            // Update response to indicate failure
            response.setData(Map.of(
                "event_created", false,
                "error", "Failed to create event: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Handle UpdateEvent tool call
     */
    private void handleUpdateEventTool(AssistantChatResponse response) {
        try {
            Map<String, Object> data = response.getData();
            @SuppressWarnings("unchecked")
            Map<String, Object> validatedData = (Map<String, Object>) data.get("validated_data");
            String eventId = (String) data.get("event_id");
            
            if (eventId == null) {
                // Try to get event ID from response
                eventId = response.getEventId();
            }
            
            if (eventId == null) {
                throw new RuntimeException("Event ID not provided for update");
            }
            
            // Call EventService to update the event
            EventResponse eventResponse = eventService.updateEvent(eventId, validatedData);
            
            // Update response with event details
            response.setEventId(eventResponse.getId().toString());
            response.setData(Map.of(
                "event_updated", true,
                "event_id", eventResponse.getId().toString(),
                "event_name", eventResponse.getName(),
                "event_type", eventResponse.getEventType()
            ));
            
            System.out.println("✅ Event updated successfully: " + eventResponse.getId());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to update event: " + e.getMessage());
            // Update response to indicate failure
            response.setData(Map.of(
                "event_updated", false,
                "error", "Failed to update event: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get chat history
     */
    @GetMapping("/chat/{chatId}/messages")
    public ResponseEntity<?> getChatMessages(@PathVariable String chatId) {
        try {
            return ResponseEntity.ok(shadeAssistantService.getChatMessages(chatId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get user chats
     */
    @GetMapping("/chats/{userId}")
    public ResponseEntity<?> getUserChats(@PathVariable String userId) {
        try {
            return ResponseEntity.ok(shadeAssistantService.getUserChats(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get event by chat ID
     */
    @GetMapping("/event/chat/{chatId}")
    public ResponseEntity<?> getEventByChatId(@PathVariable String chatId) {
        try {
            EventResponse event = eventService.getEventByChatId(chatId);
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Select a venue for an event
     */
    @PostMapping("/event/{eventId}/venue")
    public ResponseEntity<?> selectVenue(@PathVariable String eventId, @RequestBody Map<String, Object> venueData) {
        try {
            // Update event with selected venue
            EventResponse eventResponse = eventService.updateEvent(eventId, Map.of(
                "venue", venueData.get("name"),
                "location", venueData.get("address"),
                "venueDetails", venueData
            ));
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Venue selected successfully",
                "event", eventResponse
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Failed to select venue: " + e.getMessage()
            ));
        }
    }
}
