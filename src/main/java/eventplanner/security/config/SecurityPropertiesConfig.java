package eventplanner.security.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        ServiceAuthProperties.class,
        SecurityJwtProperties.class,
        RbacPolicyProperties.class,
        AwsCognitoProperties.class,
        ResourceServerJwtProperties.class
})
public class SecurityPropertiesConfig {
}
