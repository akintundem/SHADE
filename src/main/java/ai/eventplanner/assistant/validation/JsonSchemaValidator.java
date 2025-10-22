package ai.eventplanner.assistant.validation;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * JSON schema validator for external service responses
 */
@Component
public class JsonSchemaValidator {
    
    /**
     * Validate AssistantChatResponse JSON structure
     */
    public boolean validateAssistantResponse(JsonNode json) {
        if (json == null || !json.isObject()) {
            return false;
        }
        
        // Required fields
        if (!json.has("reply") || !json.get("reply").isTextual()) {
            return false;
        }
        
        // Validate reply content
        String reply = json.get("reply").asText();
        if (reply.length() > 10000) { // Max 10k characters
            return false;
        }
        
        // Check for potentially malicious content
        if (containsMaliciousContent(reply)) {
            return false;
        }
        
        // Validate optional fields if present
        if (json.has("chat_id") && !json.get("chat_id").isTextual()) {
            return false;
        }
        
        if (json.has("user_id") && !json.get("user_id").isTextual()) {
            return false;
        }
        
        if (json.has("event_id") && !json.get("event_id").isTextual()) {
            return false;
        }
        
        // Validate data field if present
        if (json.has("data") && !json.get("data").isNull()) {
            if (!validateDataField(json.get("data"))) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validate data field structure
     */
    private boolean validateDataField(JsonNode data) {
        if (!data.isObject()) {
            return false;
        }
        
        // Check for reasonable field names and values
        data.fieldNames().forEachRemaining(fieldName -> {
            if (fieldName.length() > 100) { // Field name too long
                throw new IllegalArgumentException("Invalid field name: " + fieldName);
            }
        });
        
        return true;
    }
    
    /**
     * Check for potentially malicious content
     */
    private boolean containsMaliciousContent(String content) {
        // Check for script tags
        if (Pattern.compile("<script.*?>.*?</script>", Pattern.CASE_INSENSITIVE).matcher(content).find()) {
            return true;
        }
        
        // Check for javascript: URLs
        if (Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE).matcher(content).find()) {
            return true;
        }
        
        // Check for SQL injection patterns
        if (Pattern.compile("(union|select|insert|update|delete|drop|create|alter)\\s+", Pattern.CASE_INSENSITIVE).matcher(content).find()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Sanitize JSON content
     */
    public String sanitizeContent(String content) {
        if (content == null) {
            return null;
        }
        
        // Remove potentially dangerous characters
        return content.replaceAll("[<>\"'&]", "")
                      .replaceAll("javascript:", "")
                      .trim();
    }
}
