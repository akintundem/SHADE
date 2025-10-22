package ai.eventplanner.auth.service;

import ai.eventplanner.auth.entity.ClientApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Internal service for managing client applications
 * This service is NOT exposed to the public API and should only be used by:
 * - System administrators
 * - Application initialization
 * - Internal system processes
 */
@Service
@RequiredArgsConstructor
public class InternalClientManagementService implements CommandLineRunner {

    private final ClientValidationService clientValidationService;

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
                if (!clientValidationService.isValidClient(clientInfo.clientId)) {
                    clientValidationService.createClientInternal(
                        clientInfo.clientId,
                        clientInfo.clientName,
                        clientInfo.clientType,
                        clientInfo.description
                    );
                } else {
                }
            } catch (Exception e) {
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
        // Implementation would go here
    }

    /**
     * Get all client applications (ADMIN ONLY)
     */
    public List<ClientApplication> getAllClients() {
        // Implementation would go here - for now just return empty list
        return List.of();
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
