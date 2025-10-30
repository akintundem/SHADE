package ai.eventplanner.auth.service;

import ai.eventplanner.auth.entity.ClientApplication;
import ai.eventplanner.auth.repo.ClientApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Internal service for managing client applications
 * This service is NOT exposed to the public API and should only be used by:
 * - System administrators
 * - Application initialization
 * - Internal system processes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InternalClientManagementService implements CommandLineRunner {

    private final ClientValidationService clientValidationService;
    private final ClientApplicationRepository clientApplicationRepository;

    /**
     * Initialize default clients on application startup
     * This runs automatically when the application starts
     */
    @Override
    public void run(String... args) throws Exception {
        initializeDefaultClients();
    }

    /**
     * Initialize default client applications
     * This method creates the standard clients that the system needs
     */
    private void initializeDefaultClients() {

        List<ClientInfo> defaultClients = List.of(
            new ClientInfo("web-app", "Web Application", "WEB", "Main web application client"),
            new ClientInfo("mobile-app", "Mobile Application", "MOBILE", "Mobile app client"),
            new ClientInfo("api-client", "API Client", "API", "Third-party API client"),
            new ClientInfo("desktop-app", "Desktop Application", "DESKTOP", "Desktop application client")
        );

        for (ClientInfo clientInfo : defaultClients) {
            try {
                clientApplicationRepository.findByClientId(clientInfo.clientId)
                    .ifPresentOrElse(
                        existing -> ensureClientUpToDate(existing, clientInfo),
                        () -> {
                            ClientApplication created = clientValidationService.createClientInternal(
                                clientInfo.clientId,
                                clientInfo.clientName,
                                clientInfo.clientType,
                                clientInfo.description
                            );
                            log.info("Initialized default client '{}'", created.getClientId());
                        });
            } catch (Exception e) {
                log.error("Failed to initialize default client '{}': {}", clientInfo.clientId, e.getMessage(), e);
            }
        }

    }

    /**
     * Create a new client application (ADMIN ONLY)
     * This method should only be called by system administrators
     */
    public ClientApplication createClient(String clientId, String clientName, String clientType, String description) {
        return clientValidationService.createClientInternal(clientId, clientName, clientType, description);
    }

    /**
     * Deactivate a client application (ADMIN ONLY)
     */
    public void deactivateClient(String clientId) {
        ClientApplication client = clientApplicationRepository.findByClientId(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        if (!client.isActive()) {
            log.debug("Client '{}' already inactive", clientId);
            return;
        }

        client.setActive(false);
        clientApplicationRepository.save(client);
        log.info("Deactivated client '{}'", clientId);
    }

    /**
     * Get all client applications (ADMIN ONLY)
     */
    public List<ClientApplication> getAllClients() {
        return clientApplicationRepository.findAll();
    }

    private void ensureClientUpToDate(ClientApplication existing, ClientInfo definition) {
        boolean updated = false;

        if (!existing.isActive()) {
            existing.setActive(true);
            updated = true;
        }
        if (!Objects.equals(existing.getClientName(), definition.clientName)) {
            existing.setClientName(definition.clientName);
            updated = true;
        }
        if (!Objects.equals(existing.getClientType(), definition.clientType)) {
            existing.setClientType(definition.clientType);
            updated = true;
        }
        if (!Objects.equals(existing.getDescription(), definition.description)) {
            existing.setDescription(definition.description);
            updated = true;
        }
        if (existing.getRateLimitPerMinute() == null) {
            existing.setRateLimitPerMinute(100);
            updated = true;
        }
        if (existing.getRateLimitPerHour() == null) {
            existing.setRateLimitPerHour(1000);
            updated = true;
        }
        if (existing.getMaxConcurrentSessions() == null) {
            existing.setMaxConcurrentSessions(5);
            updated = true;
        }

        // keep track of the last time the default was revalidated to aid debugging
        existing.setLastUsed(LocalDateTime.now());

        if (updated) {
            clientApplicationRepository.save(existing);
            log.info("Updated default client '{}'", existing.getClientId());
        } else {
            log.debug("Default client '{}' already up to date", existing.getClientId());
        }
    }

    /**
     * Internal data class for client information
     */
    private static class ClientInfo {
        final String clientId;
        final String clientName;
        final String clientType;
        final String description;

        ClientInfo(String clientId, String clientName, String clientType, String description) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.clientType = clientType;
            this.description = description;
        }
    }
}
