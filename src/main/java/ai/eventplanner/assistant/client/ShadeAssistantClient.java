package ai.eventplanner.assistant.client;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;

/**
 * Java client example for integrating with Shade Assistant API
 * This demonstrates how to call the Python FastAPI service from Java
 */
public class ShadeAssistantClient {
    
    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public ShadeAssistantClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Send a message to the chat and get response
     */
    public ChatResponse sendMessage(String message, String userId, String chatId, String eventId) {
        String url = baseUrl + "/chat";
        
        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", message);
        requestBody.put("user_id", userId);
        if (chatId != null) {
            requestBody.put("chat_id", chatId);
        }
        if (eventId != null) {
            requestBody.put("event_id", eventId);
        }
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return parseChatResponse(jsonResponse);
            } else {
                throw new RuntimeException("API call failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error calling Shade Assistant API", e);
        }
    }
    
    /**
     * Get all chats for a user
     */
    public JsonNode getUserChats(String userId) {
        String url = baseUrl + "/chats/" + userId;
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Error getting user chats", e);
        }
    }
    
    /**
     * Get specific chat by ID
     */
    public JsonNode getChat(String chatId) {
        String url = baseUrl + "/chat/" + chatId;
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Error getting chat", e);
        }
    }
    
    /**
     * Get messages for a chat
     */
    public JsonNode getChatMessages(String chatId) {
        String url = baseUrl + "/chat/" + chatId + "/messages";
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Error getting chat messages", e);
        }
    }
    
    /**
     * Get chat for a specific event
     */
    public JsonNode getEventChat(String eventId) {
        String url = baseUrl + "/event/" + eventId + "/chat";
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Error getting event chat", e);
        }
    }
    
    /**
     * Parse JSON response to ChatResponse object
     */
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
            response.setData(json.get("data"));
        }
        
        if (json.has("show_chips")) {
            response.setShowChips(json.get("show_chips").asBoolean());
        }
        
        return response;
    }
    
    /**
     * Chat Response DTO
     */
    public static class ChatResponse {
        private String reply;
        private String toolUsed;
        private String chatId;
        private String userId;
        private String eventId;
        private JsonNode data;
        private boolean showChips;
        
        // Getters and Setters
        public String getReply() { return reply; }
        public void setReply(String reply) { this.reply = reply; }
        
        public String getToolUsed() { return toolUsed; }
        public void setToolUsed(String toolUsed) { this.toolUsed = toolUsed; }
        
        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }
        
        public JsonNode getData() { return data; }
        public void setData(JsonNode data) { this.data = data; }
        
        public boolean isShowChips() { return showChips; }
        public void setShowChips(boolean showChips) { this.showChips = showChips; }
    }
    
    /**
     * Example usage
     */
    public static void main(String[] args) {
        ShadeAssistantClient client = new ShadeAssistantClient("http://localhost:8000");
        
        try {
            // Start a new conversation
            ChatResponse response1 = client.sendMessage("Create an event", "user123", null, null);
            System.out.println("Chat ID: " + response1.getChatId());
            System.out.println("Response: " + response1.getReply());
            
            // Continue the conversation
            ChatResponse response2 = client.sendMessage("ASHA 2025", "user123", response1.getChatId(), null);
            System.out.println("Response: " + response2.getReply());
            
            // Get chat history
            JsonNode messages = client.getChatMessages(response1.getChatId());
            System.out.println("Chat Messages: " + messages);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
