package ai.eventplanner.pushnotification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Firebase Configuration for FCM Push Notifications
 */
@Configuration
@Slf4j
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

        if (!StringUtils.hasText(serviceAccountKey) || !StringUtils.hasText(projectId)) {
            log.warn("Firebase configuration is missing. Push notifications will be disabled.");
            return null;
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
        if (firebaseApp == null) {
            log.warn("Firebase app is not initialized. Push notifications will be disabled.");
            return null;
        }
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}

