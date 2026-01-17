package eventplanner.security.auth.service;

import eventplanner.security.config.AwsCognitoProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUserGlobalSignOutRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal Cognito helper for user lifecycle operations.
 */
@Service
public class CognitoUserService {

    private final String userPoolId;
    private final Region region;

    public CognitoUserService(AwsCognitoProperties cognitoProperties) {
        String userPoolId = cognitoProperties.getUserPoolId();
        String region = cognitoProperties.getRegion();
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

    /**
     * Revoke Cognito sessions/tokens for a user. No-ops if configuration is missing.
     */
    public void signOutUser(String cognitoSub, String usernameFallback) {
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
            client.adminUserGlobalSignOut(AdminUserGlobalSignOutRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build());
        } catch (CognitoIdentityProviderException ex) {
        } catch (Exception ex) {
        }
    }

    /**
     * Best-effort update of Cognito profile attributes for user pool accounts.
     */
    public void updateUserProfile(String cognitoSub,
                                  String usernameFallback,
                                  String name,
                                  String preferredUsername,
                                  String phoneNumber) {
        String username = StringUtils.hasText(cognitoSub) ? cognitoSub : usernameFallback;
        if (!StringUtils.hasText(username)) {
            return;
        }
        if (userPoolId == null || region == null) {
            return;
        }

        List<AttributeType> attributes = new ArrayList<>();
        if (StringUtils.hasText(name)) {
            attributes.add(AttributeType.builder().name("name").value(name.trim()).build());
        }
        if (StringUtils.hasText(preferredUsername)) {
            attributes.add(AttributeType.builder().name("preferred_username").value(preferredUsername.trim()).build());
        }
        if (StringUtils.hasText(phoneNumber)) {
            attributes.add(AttributeType.builder().name("phone_number").value(phoneNumber.trim()).build());
        }

        if (attributes.isEmpty()) {
            return;
        }

        try (CognitoIdentityProviderClient client = CognitoIdentityProviderClient.builder()
                .region(region)
                .build()) {
            client.adminUpdateUserAttributes(AdminUpdateUserAttributesRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .userAttributes(attributes)
                    .build());
        } catch (CognitoIdentityProviderException ex) {
        } catch (Exception ex) {
        }
    }
}
