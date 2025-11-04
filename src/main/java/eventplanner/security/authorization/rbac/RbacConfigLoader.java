package eventplanner.security.authorization.rbac;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads the RBAC configuration from YAML.
 */
@Component
@Slf4j
public class RbacConfigLoader {

    private final ObjectMapper objectMapper;
    private final Resource configResource;

    public RbacConfigLoader(@Value("${rbac.config.location:classpath:rbac/rbac-config.yml}") String location,
                            ResourceLoader resourceLoader) {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        this.objectMapper.findAndRegisterModules();
        this.configResource = resourceLoader.getResource(location);
    }

    public RbacConfig load() {
        if (!configResource.exists()) {
            throw new IllegalStateException("RBAC configuration not found at " + configResource);
        }

        try (InputStream inputStream = configResource.getInputStream()) {
            RbacConfig config = objectMapper.readValue(inputStream, RbacConfig.class);
            log.info("Loaded RBAC configuration with {} route definitions", config.getRoutes().size());
            return config;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load RBAC configuration from " + configResource, e);
        }
    }
}
