package eventplanner.security.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

/**
 * Minimal Cognito helper for user lifecycle operations.
 */
@Service
public class CognitoUserService {

    private final String userPoolId;
    private final Region region;

    public CognitoUserService(
            @Value("${aws.cognito.user-pool-id:}") String userPoolId,
            @Value("${aws.cognito.region:}") String region) {
        this.userPoolId = StringUtils.hasText(userPoolId) ? userPoolId : null;
        this.region = StringUtils.hasText(region) ? Region.of(region) : null;
    }

    /**
     * Delete a Cognito user by subject/username. No-ops if configuration is missing.
     */
    public void deleteUser(String cognitoSub, String usernameFallback) {
        String username = StringUtils.hasText(cognitoSub) ? cognitoSub : usernameFallback;
        if (!StringUtils.hasText(username)) {
            return;
        }
        if (userPoolId == null || region == null) {
            return;
        }

        try (CognitoIdentityProviderClient client = CognitoIdentityProviderClient.builder()
                .region(region)
                .build()) {
            client.adminDeleteUser(AdminDeleteUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build());
        } catch (CognitoIdentityProviderException ex) {
        } catch (Exception ex) {
        }
    }
}
