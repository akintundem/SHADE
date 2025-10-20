package ai.eventplanner.assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import ai.eventplanner.assistant.dto.ChatResponse;
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
    public ChatResponse sendMessage(String message, String userId, String chatId, String eventId) {
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
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return parseChatResponse(jsonResponse);
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
    
    private ChatResponse parseChatResponse(JsonNode json) {
        ChatResponse response = new ChatResponse();
        response.setReply(json.get("reply").asText());
        response.setToolUsed(json.get("tool_used").asText());
        response.setChatId(json.get("chat_id").asText());
        response.setUserId(json.get("user_id").asText());
        
        if (json.has("event_id") && !json.get("event_id").isNull()) {
            response.setEventId(json.get("event_id").asText());
        }
        
        if (json.has("data") && !json.get("data").isNull()) {
            // Convert JsonNode to Map<String, Object>
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) objectMapper.convertValue(json.get("data"), Map.class);
            response.setData(dataMap);
        }
        
        if (json.has("show_chips")) {
            response.setShowChips(json.get("show_chips").asBoolean());
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
