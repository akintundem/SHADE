package eventplanner.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class for environment and profile checks.
 * Provides centralized methods to determine the current runtime environment.
 */
@Component
public class EnvironmentUtil {

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    /**
     * Checks if the current environment is development.
     * 
     * @return true if the current environment is development, false otherwise
     */
    public boolean isDevelopmentEnvironment() {
        return "dev".equals(activeProfile) || "development".equals(activeProfile);
    }

    /**
     * Checks if the current environment is production.
     * 
     * @return true if the current environment is production, false otherwise
     */
    public boolean isProductionEnvironment() {
        return "prod".equals(activeProfile) || "production".equals(activeProfile);
    }

    /**
     * Checks if the current environment is test.
     * 
     * @return true if the current environment is test, false otherwise
     */
    public boolean isTestEnvironment() {
        return "test".equals(activeProfile);
    }

    /**
     * Gets the active profile name.
     * 
     * @return the active profile name
     */
    public String getActiveProfile() {
        return activeProfile;
    }
}

