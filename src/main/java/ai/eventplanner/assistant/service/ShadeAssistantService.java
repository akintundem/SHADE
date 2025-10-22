package ai.eventplanner.assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import ai.eventplanner.assistant.dto.AssistantChatResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Spring Boot service for integrating with Shade Assistant
 * This service handles all communication with the Python FastAPI service
 */
@Service
public class ShadeAssistantService {
    
    @Value("${shade.assistant.url:http://localhost:8000}")
    private String shadeAssistantUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public ShadeAssistantService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Send a message to the chat and get response
     */
    public AssistantChatResponse sendMessage(String message, String userId, String chatId, String eventId, String correlationId) {
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
        // Internal service auth header (shared secret or JWT); secret should be provided via env/config
        String internalSecret = System.getenv().getOrDefault("INTERNAL_ASSISTANT_SECRET", "");
        if (internalSecret.isEmpty()) {
            throw new IllegalStateException("INTERNAL_ASSISTANT_SECRET environment variable must be set");
        }
        headers.add("X-Internal-Service-Auth", internalSecret);
        if (correlationId != null && !correlationId.isBlank()) {
            headers.add("X-Correlation-Id", correlationId);
        }
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return parseAssistantResponse(jsonResponse);
            } else {
                throw new RuntimeException("Shade Assistant API call failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error calling Shade Assistant API", e);
        }
    }
    
    /**
     * Get all chats for a user
     */
    public List<ChatSummary> getUserChats(String userId) {
        String url = shadeAssistantUrl + "/chats/" + userId;
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode chatsArray = jsonResponse.get("chats");
            
            List<ChatSummary> chats = new ArrayList<>();
            if (chatsArray.isArray()) {
                for (JsonNode chat : chatsArray) {
                    chats.add(parseChatSummary(chat));
                }
            }
            return chats;
        } catch (Exception e) {
            throw new RuntimeException("Error getting user chats", e);
        }
    }
    
    /**
     * Get specific chat by ID
     */
    public ChatDetails getChat(String chatId) {
        String url = shadeAssistantUrl + "/chat/" + chatId;
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return parseChatDetails(jsonResponse);
        } catch (Exception e) {
            throw new RuntimeException("Error getting chat", e);
        }
    }
    
    /**
     * Get messages for a chat
     */
    public List<Message> getChatMessages(String chatId) {
        String url = shadeAssistantUrl + "/chat/" + chatId + "/messages";
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            JsonNode messagesArray = jsonResponse.get("messages");
            
            List<Message> messages = new ArrayList<>();
            if (messagesArray.isArray()) {
                for (JsonNode message : messagesArray) {
                    messages.add(parseMessage(message));
                }
            }
            return messages;
        } catch (Exception e) {
            throw new RuntimeException("Error getting chat messages", e);
        }
    }
    
    /**
     * Get chat for a specific event
     */
    public ChatDetails getEventChat(String eventId) {
        String url = shadeAssistantUrl + "/event/" + eventId + "/chat";
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return parseChatDetails(jsonResponse);
        } catch (Exception e) {
            throw new RuntimeException("Error getting event chat", e);
        }
    }
    
    // Helper methods for parsing JSON responses
    
    private AssistantChatResponse parseAssistantResponse(JsonNode json) {
        AssistantChatResponse response = new AssistantChatResponse();
        response.setReply(json.path("reply").asText(null));
        response.setToolUsed(json.path("tool_used").asText(null));
        response.setChatId(json.path("chat_id").asText(null));
        response.setUserId(json.path("user_id").asText(null));
        response.setEventId(json.path("event_id").asText(null));
        response.setUitype(json.path("uitype").asText(null));
        response.setShowChips(json.path("show_chips").asBoolean(false));
        if (json.has("data") && !json.get("data").isNull()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) objectMapper.convertValue(json.get("data"), Map.class);
            response.setData(dataMap);
        }
        if (json.has("ui") && !json.get("ui").isNull()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> uiMap = (Map<String, Object>) objectMapper.convertValue(json.get("ui"), Map.class);
            response.setUi(uiMap);
        }
        if (json.has("structured_response") && !json.get("structured_response").isNull()) {
            var sr = new ai.eventplanner.assistant.dto.StructuredResponseDTO();
            var srNode = json.get("structured_response");
            sr.setResponseType(srNode.path("response_type").asText(null));
            sr.setText(srNode.path("text").asText(null));
            // venue_cards
            if (srNode.has("venue_cards")) {
                var list = new java.util.ArrayList<ai.eventplanner.assistant.dto.VenueCardDTO>();
                for (JsonNode v : srNode.get("venue_cards")) {
                    var vc = new ai.eventplanner.assistant.dto.VenueCardDTO();
                    vc.setId(v.path("id").asText(null));
                    vc.setName(v.path("name").asText(null));
                    vc.setLocation(v.path("location").asText(null));
                    vc.setImageUrl(v.path("image_url").asText(null));
                    if (v.has("rating") && v.get("rating").isNumber()) vc.setRating(v.get("rating").asDouble());
                    if (v.has("review_count") && v.get("review_count").isNumber()) vc.setReviewCount(v.get("review_count").asInt());
                    vc.setGuestCapacity(v.path("guest_capacity").asText(null));
                    vc.setPriceRange(v.path("price_range").asText(null));
                    vc.setDescription(v.path("description").asText(null));
                    if (v.has("amenities")) {
                        java.util.List<String> am = new java.util.ArrayList<>();
                        v.get("amenities").forEach(n -> am.add(n.asText()));
                        vc.setAmenities(am);
                    }
                    vc.setContactEmail(v.path("contact_email").asText(null));
                    vc.setContactPhone(v.path("contact_phone").asText(null));
                    vc.setWebsite(v.path("website").asText(null));
                    list.add(vc);
                }
                sr.setVenueCards(list);
            }
            // chips
            if (srNode.has("chips")) {
                var list = new java.util.ArrayList<ai.eventplanner.assistant.dto.ChipDTO>();
                for (JsonNode c : srNode.get("chips")) {
                    var chip = new ai.eventplanner.assistant.dto.ChipDTO();
                    chip.setId(c.path("id").asText(null));
                    chip.setLabel(c.path("label").asText(null));
                    chip.setIcon(c.path("icon").asText(null));
                    chip.setSelected(c.path("selected").asBoolean(false));
                    chip.setAction(c.path("action").asText(null));
                    list.add(chip);
                }
                sr.setChips(list);
            }
            // action buttons
            if (srNode.has("action_buttons")) {
                var list = new java.util.ArrayList<ai.eventplanner.assistant.dto.ActionButtonDTO>();
                for (JsonNode b : srNode.get("action_buttons")) {
                    var ab = new ai.eventplanner.assistant.dto.ActionButtonDTO();
                    ab.setId(b.path("id").asText(null));
                    ab.setLabel(b.path("label").asText(null));
                    ab.setIcon(b.path("icon").asText(null));
                    ab.setAction(b.path("action").asText(null));
                    ab.setStyle(b.path("style").asText(null));
                    ab.setDisabled(b.path("disabled").asBoolean(false));
                    list.add(ab);
                }
                sr.setActionButtons(list);
            }
            response.setStructuredResponse(sr);
        }
        return response;
    }
    
    private ChatSummary parseChatSummary(JsonNode json) {
        ChatSummary summary = new ChatSummary();
        summary.setChatId(json.get("_id").asText());
        summary.setUserId(json.get("user_id").asText());
        summary.setEventId(json.has("event_id") ? json.get("event_id").asText() : null);
        summary.setStatus(json.get("status").asText());
        summary.setCreatedAt(json.get("created_at").asText());
        summary.setUpdatedAt(json.get("updated_at").asText());
        summary.setMessageCount(json.get("messages").size());
        return summary;
    }
    
    private ChatDetails parseChatDetails(JsonNode json) {
        ChatDetails details = new ChatDetails();
        details.setChatId(json.get("_id").asText());
        details.setUserId(json.get("user_id").asText());
        details.setEventId(json.has("event_id") ? json.get("event_id").asText() : null);
        details.setStatus(json.get("status").asText());
        details.setCreatedAt(json.get("created_at").asText());
        details.setUpdatedAt(json.get("updated_at").asText());
        
        // Parse messages
        JsonNode messagesArray = json.get("messages");
        List<Message> messages = new ArrayList<>();
        if (messagesArray.isArray()) {
            for (JsonNode message : messagesArray) {
                messages.add(parseMessage(message));
            }
        }
        details.setMessages(messages);
        
        return details;
    }
    
    private Message parseMessage(JsonNode json) {
        Message message = new Message();
        message.setMessage(json.get("message").asText());
        message.setIsUser(json.get("is_user").asBoolean());
        message.setTimestamp(json.get("timestamp").asText());
        
        if (json.has("tool_used") && !json.get("tool_used").isNull()) {
            message.setToolUsed(json.get("tool_used").asText());
        }
        
        if (json.has("data") && !json.get("data").isNull()) {
            message.setData(json.get("data"));
        }
        
        return message;
    }
    
    // DTOs
    
    
    public static class ChatSummary {
        private String chatId;
        private String userId;
        private String eventId;
        private String status;
        private String createdAt;
        private String updatedAt;
        private int messageCount;
        
        // Getters and Setters
        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        
        public int getMessageCount() { return messageCount; }
        public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
    }
    
    public static class ChatDetails {
        private String chatId;
        private String userId;
        private String eventId;
        private String status;
        private String createdAt;
        private String updatedAt;
        private List<Message> messages;
        
        // Getters and Setters
        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        
        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }
    }
    
    public static class Message {
        private String message;
        private boolean isUser;
        private String timestamp;
        private String toolUsed;
        private JsonNode data;
        
        // Getters and Setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public boolean isUser() { return isUser; }
        public void setIsUser(boolean isUser) { this.isUser = isUser; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getToolUsed() { return toolUsed; }
        public void setToolUsed(String toolUsed) { this.toolUsed = toolUsed; }
        
        public JsonNode getData() { return data; }
        public void setData(JsonNode data) { this.data = data; }
    }
}
