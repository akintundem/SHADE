package ai.eventplanner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Production security configuration
 * More restrictive security settings for production environment
 */
@Configuration
@EnableWebSecurity
@Profile("prod")
public class ProductionSecurityConfig {

    @Bean
    public SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // In production, restrict access to development tools
                .requestMatchers("/h2-console/**").denyAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").denyAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // Allow only essential public endpoints
                .requestMatchers("/health", "/actuator/health").permitAll()
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/auth/forgot-password", "/api/v1/auth/reset-password").permitAll()
                .requestMatchers("/api/v1/auth/verify-email/**").permitAll()
                .requestMatchers("/api/v1/auth/validate-token", "/api/v1/auth/refresh-token").permitAll()
                .requestMatchers("/api/v1/auth/health").permitAll()
                
                // Require authentication for all other endpoints
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}
