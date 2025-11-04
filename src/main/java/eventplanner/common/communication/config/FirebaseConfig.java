package eventplanner.common.communication.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Firebase Configuration for FCM Push Notifications
 * Only loads when both firebase.service-account-key and firebase.project-id are provided and non-empty
 */
@Configuration
@Slf4j
@Conditional(FirebaseCondition.class)
public class FirebaseConfig {

    @Value("${firebase.service-account-key:}")
    private String serviceAccountKey;

    @Value("${firebase.project-id:}")
    private String projectId;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Check if Firebase is already initialized
        try {
            return FirebaseApp.getInstance();
        } catch (IllegalStateException e) {
            // Firebase not initialized, proceed with initialization
        }

        // Validate that properties are not empty and look like valid JSON
        if (!StringUtils.hasText(serviceAccountKey) || !StringUtils.hasText(projectId)) {
            log.warn("Firebase configuration is missing or empty. Push notifications will be disabled.");
            throw new IllegalStateException("Firebase service account key or project ID is missing or empty");
        }

        // Check if the service account key looks like valid JSON (should start with '{')
        String trimmedKey = serviceAccountKey.trim();
        if (!trimmedKey.startsWith("{") && !trimmedKey.startsWith("[")) {
            log.error("Firebase service account key does not appear to be valid JSON. It should be a JSON object or array.");
            throw new IllegalArgumentException("Firebase service account key must be valid JSON");
        }

        try {
            // Parse the service account JSON key
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(serviceAccountKey.getBytes(StandardCharsets.UTF_8))
            );

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase initialized successfully for project: {}", projectId);
            return app;
        } catch (Exception e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage(), e);
            throw new IOException("Failed to initialize Firebase", e);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}

