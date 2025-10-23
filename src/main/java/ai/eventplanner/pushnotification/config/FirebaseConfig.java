package ai.eventplanner.pushnotification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Firebase configuration for push notifications
 */
@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)
public class FirebaseConfig {
    
    @Value("${firebase.service-account-key:}")
    private String serviceAccountKey;
    
    @Value("${firebase.project-id:}")
    private String projectId;
    
    @Bean
    public FirebaseMessaging firebaseMessaging() throws IOException {
        if (StringUtils.hasText(serviceAccountKey) && StringUtils.hasText(projectId) && 
            !serviceAccountKey.equals("your_firebase_service_account_json_here")) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(serviceAccountKey.getBytes())
            );
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();
            FirebaseApp.initializeApp(options);
        } else {
            // Initialize with default credentials for development
            FirebaseApp.initializeApp();
        }
        return FirebaseMessaging.getInstance();
    }
}
