package ai.eventplanner.pushnotification.service;

import ai.eventplanner.pushnotification.dto.RegisterDeviceTokenRequest;
import ai.eventplanner.comms.model.DeviceToken;
import ai.eventplanner.comms.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing device tokens for push notifications
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * Register or update a device token for a user
     */
    public DeviceToken registerDeviceToken(RegisterDeviceTokenRequest request) {
        // Check if token already exists
        Optional<DeviceToken> existingToken = deviceTokenRepository.findByDeviceToken(request.getDeviceToken());
        
        if (existingToken.isPresent()) {
            DeviceToken token = existingToken.get();
            // If the token belongs to the same user, update it
            if (token.getUserId().equals(request.getUserId())) {
                token.setPlatform(request.getPlatform());
                token.setDeviceId(request.getDeviceId());
                token.setAppVersion(request.getAppVersion());
                token.setIsActive(true);
                token.markAsUsed();
                log.info("Updated existing device token for user: {}", request.getUserId());
                return deviceTokenRepository.save(token);
            } else {
                // Token belongs to a different user, deactivate old one and create new
                token.deactivate();
                deviceTokenRepository.save(token);
                log.info("Device token transferred from user {} to user {}", 
                    token.getUserId(), request.getUserId());
            }
        }

        // Create new device token
        DeviceToken newToken = new DeviceToken();
        newToken.setUserId(request.getUserId());
        newToken.setDeviceToken(request.getDeviceToken());
        newToken.setPlatform(request.getPlatform());
        newToken.setDeviceId(request.getDeviceId());
        newToken.setAppVersion(request.getAppVersion());
        newToken.setIsActive(true);
        newToken.setLastUsedAt(LocalDateTime.now());

        log.info("Registered new device token for user: {}, platform: {}", 
            request.getUserId(), request.getPlatform());
        return deviceTokenRepository.save(newToken);
    }

    /**
     * Get all active device tokens for a user
     */
    public List<DeviceToken> getUserDeviceTokens(UUID userId) {
        return deviceTokenRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Deactivate a device token
     */
    public void deactivateDeviceToken(String deviceToken) {
        Optional<DeviceToken> token = deviceTokenRepository.findByDeviceToken(deviceToken);
        if (token.isPresent()) {
            token.get().deactivate();
            deviceTokenRepository.save(token.get());
            log.info("Deactivated device token: {}", deviceToken);
        } else {
            log.warn("Device token not found: {}", deviceToken);
        }
    }

    /**
     * Deactivate all tokens for a user
     */
    public void deactivateAllUserTokens(UUID userId) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(userId);
        tokens.forEach(DeviceToken::deactivate);
        deviceTokenRepository.saveAll(tokens);
        log.info("Deactivated all device tokens for user: {}", userId);
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
}

