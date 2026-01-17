package eventplanner.common.communication.services.channel.push;

import eventplanner.common.communication.services.channel.push.dto.RefreshDeviceTokenRequest;
import eventplanner.common.communication.services.channel.push.dto.RegisterDeviceTokenRequest;
import eventplanner.common.communication.services.core.dto.PushResult;
import eventplanner.common.communication.model.DeviceToken;
import eventplanner.common.communication.repository.DeviceTokenRepository;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import eventplanner.common.config.ExternalServicesProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Service for managing device tokens and proxying push notifications to the external push microservice.
 */
@Service
@Transactional
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final RestTemplate restTemplate;
    private final String pushServiceUrl;
    private final String sharedSecret;
    private final ObjectMapper objectMapper;
    
    // Configuration for token invalidation
    private static final int MAX_TOKEN_FAILURES = 3;
    private static final int BATCH_SIZE = 1000; // Max tokens per batch to avoid payload size limits

    public PushNotificationService(DeviceTokenRepository deviceTokenRepository,
                                   ExternalServicesProperties properties,
                                   ObjectMapper objectMapper) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.restTemplate = createRestTemplate();
        String pushServiceUrl = properties.getPushService().getUrl();
        String sharedSecret = properties.getPushService().getSecret();
        if (pushServiceUrl == null || pushServiceUrl.isBlank()) {
            throw new IllegalStateException("external.push-service.url must be configured");
        }
        this.pushServiceUrl = pushServiceUrl.endsWith("/") ? pushServiceUrl.substring(0, pushServiceUrl.length() - 1) : pushServiceUrl;
        this.sharedSecret = sharedSecret;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    /**
     * Refresh an existing device token with a new token value
     */
    public DeviceToken refreshDeviceToken(RefreshDeviceTokenRequest request) {
        Optional<DeviceToken> tokenToRefresh = Optional.empty();
        
        if (request.getOldDeviceToken() != null && !request.getOldDeviceToken().isEmpty()) {
            tokenToRefresh = deviceTokenRepository.findByDeviceToken(request.getOldDeviceToken());
        } else {
            List<DeviceToken> activeTokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(request.getUserId());
            if (!activeTokens.isEmpty()) {
                tokenToRefresh = activeTokens.stream()
                        .filter(token -> token.getLastUsedAt() != null)
                        .max((t1, t2) -> t1.getLastUsedAt().compareTo(t2.getLastUsedAt()));
                if (tokenToRefresh.isEmpty() && !activeTokens.isEmpty()) {
                    tokenToRefresh = Optional.of(activeTokens.get(0));
                }
            }
        }
        
        if (tokenToRefresh.isPresent() && tokenToRefresh.get().getUserId().equals(request.getUserId())) {
            DeviceToken token = tokenToRefresh.get();
            
            Optional<DeviceToken> existingNewToken = deviceTokenRepository.findByDeviceToken(request.getDeviceToken());
            if (existingNewToken.isPresent() && !existingNewToken.get().getId().equals(token.getId())) {
                token.deactivate();
                deviceTokenRepository.save(token);
                return existingNewToken.get();
            }
            
            token.setDeviceToken(request.getDeviceToken());
            if (request.getPlatform() != null) {
                token.setPlatform(request.getPlatform());
            }
            if (request.getDeviceId() != null) {
                token.setDeviceId(request.getDeviceId());
            }
            if (request.getAppVersion() != null) {
                token.setAppVersion(request.getAppVersion());
            }
            token.setIsActive(true);
            token.markAsUsed();
            
            return deviceTokenRepository.save(token);
        }
        
        DeviceToken newToken = new DeviceToken();
        newToken.setUserId(request.getUserId());
        newToken.setDeviceToken(request.getDeviceToken());
        newToken.setPlatform(request.getPlatform() != null ? request.getPlatform() : DeviceToken.Platform.ANDROID);
        newToken.setDeviceId(request.getDeviceId());
        newToken.setAppVersion(request.getAppVersion());
        newToken.setIsActive(true);
        newToken.setLastUsedAt(LocalDateTime.now());
        
        return deviceTokenRepository.save(newToken);
    }

    /**
     * Register or update a device token for a user
     */
    public DeviceToken registerDeviceToken(RegisterDeviceTokenRequest request) {
        if (request.getUserId() == null || request.getDeviceToken() == null || request.getDeviceToken().isBlank()) {
            return null;
        }

        DeviceToken.Platform platform = request.getPlatform() != null
                ? request.getPlatform()
                : DeviceToken.Platform.ANDROID;
        Optional<DeviceToken> existingToken = deviceTokenRepository.findByDeviceToken(request.getDeviceToken());
        
        if (existingToken.isPresent()) {
            DeviceToken token = existingToken.get();
            if (token.getUserId().equals(request.getUserId())) {
                token.setPlatform(platform);
                token.setDeviceId(request.getDeviceId());
                token.setAppVersion(request.getAppVersion());
                token.setIsActive(true);
                token.markAsUsed();
                return deviceTokenRepository.save(token);
            } else {
                token.deactivate();
                deviceTokenRepository.save(token);
            }
        }

        DeviceToken newToken = new DeviceToken();
        newToken.setUserId(request.getUserId());
        newToken.setDeviceToken(request.getDeviceToken());
        newToken.setPlatform(platform);
        newToken.setDeviceId(request.getDeviceId());
        newToken.setAppVersion(request.getAppVersion());
        newToken.setIsActive(true);
        newToken.setLastUsedAt(LocalDateTime.now());

        return deviceTokenRepository.save(newToken);
    }

    /**
     * Send push notification to a specific user
     */
    public PushResult sendToNotification(UUID userId, String title, String body, Map<String, String> data) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(userId);
        
        if (tokens.isEmpty()) {
            return PushResult.builder()
                    .success(false)
                    .errorMessage("No active device tokens found for user")
                    .build();
        }
        
        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (sharedSecret != null && !sharedSecret.isBlank()) {
                headers.add("x-push-secret", sharedSecret);
            }

            var bodyPayload = new java.util.HashMap<String, Object>();
            bodyPayload.put("to", tokens.stream().map(DeviceToken::getDeviceToken).toList());
            bodyPayload.put("title", title);
            bodyPayload.put("body", body != null ? body : "");
            bodyPayload.put("data", data != null ? data : Map.of());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(bodyPayload, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    pushServiceUrl + "/send-push",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // Parse response to check for invalid tokens
                String responseBody = response.getBody();
                handlePushResponse(tokens, responseBody);
                
                tokens.forEach(token -> {
                    token.markAsUsed();
                    token.resetFailureCount();
                    deviceTokenRepository.save(token);
                });
                
                return PushResult.builder()
                    .success(true)
                    .messageId(extractMessageId(responseBody))
                    .build();
            } else {
                String errorMessage = "HTTP " + response.getStatusCode() + ": " + response.getBody();
                // Record failures for all tokens
                tokens.forEach(token -> {
                    token.recordFailure(errorMessage);
                    if (token.shouldBeInvalidated(MAX_TOKEN_FAILURES)) {
                        token.markAsInvalid("Max failures reached: " + errorMessage);
                    }
                    deviceTokenRepository.save(token);
                });
                
                return PushResult.builder()
                        .success(false)
                        .errorMessage(errorMessage)
                        .build();
            }

        } catch (RestClientException e) {
            // Record failures for all tokens
            tokens.forEach(token -> {
                token.recordFailure(e.getMessage());
                if (token.shouldBeInvalidated(MAX_TOKEN_FAILURES)) {
                    token.markAsInvalid("Max failures reached: " + e.getMessage());
                }
                deviceTokenRepository.save(token);
            });
            
            return PushResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        } catch (Exception e) {
            // Record failures for all tokens
            tokens.forEach(token -> {
                token.recordFailure(e.getMessage());
                if (token.shouldBeInvalidated(MAX_TOKEN_FAILURES)) {
                    token.markAsInvalid("Max failures reached: " + e.getMessage());
                }
                deviceTokenRepository.save(token);
            });
            
            return PushResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    /**
     * Send push notification to multiple users (bulk)
     * Handles batching and token invalidation
     */
    public BulkPushResult sendBulkNotification(List<UUID> userIds, String title, String body, Map<String, String> data) {
        BulkPushResult result = new BulkPushResult();
        
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        
        // Get all active tokens for all users
        List<DeviceToken> allTokens = deviceTokenRepository.findByUserIdInAndIsActiveTrue(userIds);
        
        if (allTokens.isEmpty()) {
            result.setTotalRecipients(0);
            result.setSuccessCount(0);
            result.setFailureCount(0);
            return result;
        }
        
        // Group tokens by user for tracking
        Map<UUID, List<DeviceToken>> tokensByUser = new java.util.HashMap<>();
        for (DeviceToken token : allTokens) {
            tokensByUser.computeIfAbsent(token.getUserId(), k -> new ArrayList<>()).add(token);
        }
        
        // Batch tokens to avoid payload size limits
        List<List<DeviceToken>> batches = batchTokens(allTokens, BATCH_SIZE);
        
        for (List<DeviceToken> batch : batches) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (sharedSecret != null && !sharedSecret.isBlank()) {
                    headers.add("x-push-secret", sharedSecret);
                }
                
                var bodyPayload = new java.util.HashMap<String, Object>();
                bodyPayload.put("to", batch.stream().map(DeviceToken::getDeviceToken).toList());
                bodyPayload.put("title", title);
                bodyPayload.put("body", body != null ? body : "");
                bodyPayload.put("data", data != null ? data : Map.of());
                
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(bodyPayload, headers);
                
                ResponseEntity<String> response = restTemplate.exchange(
                        pushServiceUrl + "/send-push",
                        HttpMethod.POST,
                        request,
                        String.class
                );
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    String responseBody = response.getBody();
                    handlePushResponse(batch, responseBody);
                    
                    batch.forEach(token -> {
                        token.markAsUsed();
                        token.resetFailureCount();
                        deviceTokenRepository.save(token);
                    });
                    
                    result.incrementSuccess(batch.size());
                } else {
                    String errorMessage = "HTTP " + response.getStatusCode() + ": " + response.getBody();
                    batch.forEach(token -> {
                        token.recordFailure(errorMessage);
                        if (token.shouldBeInvalidated(MAX_TOKEN_FAILURES)) {
                            token.markAsInvalid("Max failures reached: " + errorMessage);
                        }
                        deviceTokenRepository.save(token);
                    });
                    
                    result.incrementFailure(batch.size());
                }
            } catch (Exception e) {
                batch.forEach(token -> {
                    token.recordFailure(e.getMessage());
                    if (token.shouldBeInvalidated(MAX_TOKEN_FAILURES)) {
                        token.markAsInvalid("Max failures reached: " + e.getMessage());
                    }
                    deviceTokenRepository.save(token);
                });
                
                result.incrementFailure(batch.size());
            }
        }
        
        result.setTotalRecipients(tokensByUser.size());
        return result;
    }
    
    /**
     * Handle push service response and invalidate tokens if needed
     * In a real implementation, the push service should return invalid token IDs
     */
    private void handlePushResponse(List<DeviceToken> tokens, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return;
        }
        
        try {
            // Parse response to check for invalid tokens
            // Expected format: {"success": true, "messageId": "...", "invalidTokens": ["token1", "token2"], ...}
            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            
            @SuppressWarnings("unchecked")
            List<String> invalidTokens = (List<String>) response.get("invalidTokens");
            
            if (invalidTokens != null && !invalidTokens.isEmpty()) {
                Set<String> invalidTokenSet = new HashSet<>(invalidTokens);
                tokens.forEach(token -> {
                    if (invalidTokenSet.contains(token.getDeviceToken())) {
                        token.markAsInvalid("Token reported as invalid by push service");
                        deviceTokenRepository.save(token);
                    }
                });
            }
        } catch (Exception e) {
            // Log but don't fail - response parsing is best effort
            // In production, you'd want proper logging here
        }
    }
    
    private String extractMessageId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            Object messageId = response.get("messageId");
            return messageId != null ? messageId.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private List<List<DeviceToken>> batchTokens(List<DeviceToken> tokens, int batchSize) {
        List<List<DeviceToken>> batches = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i += batchSize) {
            batches.add(tokens.subList(i, Math.min(i + batchSize, tokens.size())));
        }
        return batches;
    }
    
    /**
     * Result class for bulk push operations
     */
    public static class BulkPushResult {
        private int totalRecipients = 0;
        private int successCount = 0;
        private int failureCount = 0;
        private String messageId;
        private String errorMessage;
        
        public void incrementSuccess(int count) {
            this.successCount += count;
        }
        
        public void incrementFailure(int count) {
            this.failureCount += count;
        }
        
        // Getters and setters
        public int getTotalRecipients() { return totalRecipients; }
        public void setTotalRecipients(int totalRecipients) { this.totalRecipients = totalRecipients; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public boolean isSuccess() {
            return failureCount == 0 && successCount > 0;
        }
    }
}
