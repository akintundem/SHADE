package eventplanner.common.communication.services.channel.push;

import eventplanner.common.communication.services.channel.push.dto.RefreshDeviceTokenRequest;
import eventplanner.common.communication.services.channel.push.dto.RegisterDeviceTokenRequest;
import eventplanner.common.communication.services.core.dto.PushResult;
import eventplanner.common.communication.model.DeviceToken;
import eventplanner.common.communication.repository.DeviceTokenRepository;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Value;
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

    public PushNotificationService(DeviceTokenRepository deviceTokenRepository,
                                   @Value("${external.push-service.url:http://shade-push-service:3100}") String pushServiceUrl,
                                   @Value("${external.push-service.secret:}") String sharedSecret) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.restTemplate = createRestTemplate();
        this.pushServiceUrl = pushServiceUrl.endsWith("/") ? pushServiceUrl.substring(0, pushServiceUrl.length() - 1) : pushServiceUrl;
        this.sharedSecret = sharedSecret;
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
     * Deactivate all tokens for a user
     */
    public void deactivateAllUserTokens(UUID userId) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(userId);
        tokens.forEach(DeviceToken::deactivate);
        deviceTokenRepository.saveAll(tokens);
    }

    /**
     * Update last used timestamp for a device token
     */
    public void updateLastUsed(String deviceToken) {
        Optional<DeviceToken> token = deviceTokenRepository.findByDeviceToken(deviceToken);
        if (token.isPresent()) {
            token.get().markAsUsed();
            deviceTokenRepository.save(token.get());
        }
    }

    /**
     * Send push notification to a specific user
     */
    public PushResult sendToNotification(UUID userId, String title, String body, Map<String, String> data) {
        try {
            List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(userId);
            
            if (tokens.isEmpty()) {
                return PushResult.builder()
                        .success(false)
                        .errorMessage("No active device tokens found for user")
                        .build();
            }

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
                tokens.forEach(token -> updateLastUsed(token.getDeviceToken()));
                return PushResult.builder()
                    .success(true)
                    .messageId(null)
                    .build();
            } else {
                String errorMessage = "HTTP " + response.getStatusCode() + ": " + response.getBody();
                return PushResult.builder()
                        .success(false)
                        .errorMessage(errorMessage)
                        .build();
            }

        } catch (RestClientException e) {
            return PushResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        } catch (Exception e) {
            return PushResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
