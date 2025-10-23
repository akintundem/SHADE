package ai.eventplanner.pushnotification.service;

import ai.eventplanner.comms.model.DeviceToken;
import ai.eventplanner.comms.repository.DeviceTokenRepository;
import ai.eventplanner.pushnotification.dto.RegisterDeviceTokenRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing device tokens
 */
@Service
public class DeviceTokenService {
    
    private final DeviceTokenRepository deviceTokenRepository;
    
    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }
    
    /**
     * Register a new device token
     */
    public DeviceToken registerToken(RegisterDeviceTokenRequest request) {
        // Check if token already exists
        Optional<DeviceToken> existingToken = deviceTokenRepository.findByDeviceToken(request.getDeviceToken());
        
        if (existingToken.isPresent()) {
            DeviceToken token = existingToken.get();
            // Update existing token
            token.setUserId(request.getUserId());
            token.setPlatform(request.getPlatform());
            token.setDeviceId(request.getDeviceId());
            token.setAppVersion(request.getAppVersion());
            token.setIsActive(true);
            token.markAsUsed();
            return deviceTokenRepository.save(token);
        } else {
            // Create new token
            DeviceToken token = new DeviceToken(
                request.getUserId(),
                request.getDeviceToken(),
                request.getPlatform()
            );
            token.setDeviceId(request.getDeviceId());
            token.setAppVersion(request.getAppVersion());
            return deviceTokenRepository.save(token);
        }
    }
    
    /**
     * Get active tokens for a user
     */
    public List<DeviceToken> getUserTokens(UUID userId) {
        return deviceTokenRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    /**
     * Deactivate a device token
     */
    public void deactivateToken(String deviceToken) {
        Optional<DeviceToken> token = deviceTokenRepository.findByDeviceToken(deviceToken);
        if (token.isPresent()) {
            token.get().deactivate();
            deviceTokenRepository.save(token.get());
        }
    }
    
    /**
     * Clean up old inactive tokens
     */
    public int cleanupOldTokens() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        return deviceTokenRepository.deactivateOldTokens(cutoffDate);
    }
    
    /**
     * Get token count for a user
     */
    public long getUserTokenCount(UUID userId) {
        return deviceTokenRepository.countByUserIdAndIsActiveTrue(userId);
    }
}
