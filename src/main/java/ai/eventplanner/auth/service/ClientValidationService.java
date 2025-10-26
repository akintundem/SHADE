package ai.eventplanner.auth.service;

import ai.eventplanner.auth.entity.ClientApplication;
import ai.eventplanner.auth.repo.ClientApplicationRepository;
import ai.eventplanner.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientValidationService {

    private final ClientApplicationRepository clientApplicationRepository;

    /**
     * Validate client ID and return client application if valid
     */
    public ClientApplication validateClientId(String clientId) {
        Optional<ClientApplication> clientOpt = clientApplicationRepository.findActiveByClientId(clientId);
        if (clientOpt.isEmpty()) {
            throw new UnauthorizedException("Invalid client ID");
        }

        ClientApplication client = clientOpt.get();
        
        // Update last used timestamp
        client.updateLastUsed();
        return client;
    }

    /**
     * Check if client ID exists and is active (without updating timestamp)
     */
    public boolean isValidClient(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return false;
        }
        return clientApplicationRepository.existsActiveByClientId(clientId);
    }

    /**
     * Get client application by ID
     */
    public Optional<ClientApplication> getClientById(String clientId) {
        return clientApplicationRepository.findActiveByClientId(clientId);
    }

    /**
     * Create a new client application (INTERNAL USE ONLY)
     * This method should only be used by administrators or during system initialization
     */
    public ClientApplication createClientInternal(String clientId, String clientName, String clientType, String description) {
        if (clientApplicationRepository.findByClientId(clientId).isPresent()) {
            throw new IllegalArgumentException("Client ID already exists");
        }

        ClientApplication client = ClientApplication.builder()
                .clientId(clientId)
                .clientName(clientName)
                .clientSecretHash("")
                .clientType(clientType)
                .active(true)
                .description(description)
                .lastUsed(LocalDateTime.now())
                .rateLimitPerMinute(100)
                .rateLimitPerHour(1000)
                .maxConcurrentSessions(5)
                .build();

        return clientApplicationRepository.save(client);
    }
}
