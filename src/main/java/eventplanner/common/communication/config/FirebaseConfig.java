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

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Firebase Configuration for FCM Push Notifications
 * Only loads when both firebase.service-account-key-path and firebase.project-id are provided and non-empty
 */
@Configuration
@Slf4j
@Conditional(FirebaseCondition.class)
public class FirebaseConfig {

    @Value("${firebase.service-account-key-path:}")
    private String serviceAccountKeyPath;

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

        // Validate that properties are not empty
        if (!StringUtils.hasText(serviceAccountKeyPath) || !StringUtils.hasText(projectId)) {
            log.warn("Firebase configuration is missing or empty. Push notifications will be disabled.");
            throw new IllegalStateException("Firebase service account key path or project ID is missing or empty");
        }

        try {
            // Load service account key from file
            FileInputStream serviceAccount = new FileInputStream(serviceAccountKeyPath);

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
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

