package eventplanner.common.communication.services.channel.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import eventplanner.common.communication.model.DeviceToken;
import eventplanner.common.communication.repository.DeviceTokenRepository;
import eventplanner.common.communication.services.channel.push.dto.RefreshDeviceTokenRequest;
import eventplanner.common.communication.services.channel.push.dto.RegisterDeviceTokenRequest;
import eventplanner.common.communication.services.channel.push.dto.PushJobRequest;
import eventplanner.common.communication.services.core.dto.PushResult;
import eventplanner.common.config.RabbitMqProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing device tokens and publishing push jobs to RabbitMQ.
 */
@Service
@Transactional
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final ObjectMapper objectMapper;

    // Configuration for token invalidation
    private static final int MAX_TOKEN_FAILURES = 3;
    private static final int BATCH_SIZE = 500; // Cap per provider limits; reduce abuse blast radius

    public PushNotificationService(DeviceTokenRepository deviceTokenRepository,
                                   RabbitTemplate rabbitTemplate,
                                   RabbitMqProperties rabbitMqProperties,
                                   ObjectMapper objectMapper) {
        String exchange = rabbitMqProperties.getExchange();
        String routingKey = rabbitMqProperties.getPushRoutingKey();
        if (exchange == null || exchange.isBlank()) {
            throw new IllegalStateException("rabbitmq.exchange must be configured");
        }
        if (routingKey == null || routingKey.isBlank()) {
            throw new IllegalStateException("rabbitmq.push-routing-key must be configured");
        }
        this.deviceTokenRepository = deviceTokenRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
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
            List<String> recipients = tokens.stream().map(DeviceToken::getDeviceToken).toList();
            String messageId = publishPushJob(recipients, title, body, data);

            tokens.forEach(token -> {
                token.markAsUsed();
                token.resetFailureCount();
                deviceTokenRepository.save(token);
            });

            return PushResult.builder()
                .success(true)
                .messageId(messageId)
                .build();
        } catch (Exception e) {
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
        Map<UUID, List<DeviceToken>> tokensByUser = new HashMap<>();
        for (DeviceToken token : allTokens) {
            tokensByUser.computeIfAbsent(token.getUserId(), k -> new ArrayList<>()).add(token);
        }
        
        // Batch tokens to avoid payload size limits
        List<List<DeviceToken>> batches = batchTokens(allTokens, BATCH_SIZE);
        
        for (List<DeviceToken> batch : batches) {
            try {
                List<String> recipients = batch.stream().map(DeviceToken::getDeviceToken).toList();
                publishPushJob(recipients, title, body, data);

                batch.forEach(token -> {
                    token.markAsUsed();
                    token.resetFailureCount();
                    deviceTokenRepository.save(token);
                });

                result.incrementSuccess(batch.size());
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

    private String publishPushJob(List<String> recipients, String title, String body, Map<String, String> data) throws Exception {
        PushJobRequest payload = PushJobRequest.builder()
                .to(recipients)
                .title(title)
                .body(body != null ? body : "")
                .data(data != null ? data : Map.of())
                .build();

        String payloadJson = objectMapper.writeValueAsString(payload);
        String messageId = UUID.randomUUID().toString();

        rabbitTemplate.convertAndSend(exchange, routingKey, payloadJson, message -> {
            message.getMessageProperties().setContentType(MediaType.APPLICATION_JSON_VALUE);
            message.getMessageProperties().setMessageId(messageId);
            return message;
        });

        return messageId;
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
