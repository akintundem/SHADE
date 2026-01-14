package eventplanner.security.config;

import eventplanner.security.auth.jwt.CognitoJwtAuthenticationConverter;
import eventplanner.security.filters.RbacContextFilter;
import eventplanner.security.filters.SecurityHeadersFilter;
import eventplanner.security.filters.ServiceApiKeyFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final SecurityHeadersFilter securityHeadersFilter;
    private final RbacContextFilter rbacContextFilter;
    private final ServiceApiKeyFilter serviceApiKeyFilter;
    private final CognitoJwtAuthenticationConverter jwtAuthenticationConverter;
    private final String issuerUri;
    private final String jwkSetUri;
    private final String audienceProperty;

    public SecurityConfig(SecurityHeadersFilter securityHeadersFilter,
                          RbacContextFilter rbacContextFilter,
                          ServiceApiKeyFilter serviceApiKeyFilter,
                          CognitoJwtAuthenticationConverter jwtAuthenticationConverter,
                          @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri,
                          @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
                          @Value("${spring.security.oauth2.resourceserver.jwt.audiences:}") String audienceProperty) {
        this.securityHeadersFilter = securityHeadersFilter;
        this.rbacContextFilter = rbacContextFilter;
        this.serviceApiKeyFilter = serviceApiKeyFilter;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.issuerUri = issuerUri;
        this.jwkSetUri = jwkSetUri;
        this.audienceProperty = audienceProperty;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Perimeter CORS is enforced at the gateway; disable Spring CORS here.
            .cors(AbstractHttpConfigurer::disable)
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher("/api/**")
                )) // Disable CSRF for all API endpoints (using JWT auth)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/metrics").authenticated()
                .requestMatchers("/error", "/favicon.ico", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").authenticated()
                .requestMatchers(HttpMethod.GET, "/").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable())
            // Custom exception handling to prevent leaking internal details
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(401);
                    response.getWriter().write("{\"timestamp\":\"" + java.time.LocalDateTime.now() + "\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"path\":\"" + request.getRequestURI() + "\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json");
                    response.setStatus(403);
                    response.getWriter().write("{\"timestamp\":\"" + java.time.LocalDateTime.now() + "\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied\",\"path\":\"" + request.getRequestURI() + "\"}");
                })
            );

        http.addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(serviceApiKeyFilter, SecurityHeadersFilter.class);
        http.addFilterAfter(rbacContextFilter, BearerTokenAuthenticationFilter.class);

        if (StringUtils.hasText(issuerUri) || StringUtils.hasText(jwkSetUri)) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            );
        } else {
            http.oauth2ResourceServer(oauth2 -> oauth2.disable());
        }

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String issuer = issuerUri;
        String jwks = jwkSetUri;
        String audience = audienceProperty;

        if (!StringUtils.hasText(jwks) && !StringUtils.hasText(issuer)) {
            return token -> { throw new org.springframework.security.oauth2.jwt.JwtException("JWT validation is disabled (no issuer configured)"); };
        }

        NimbusJwtDecoder decoder = StringUtils.hasText(jwks)
                ? NimbusJwtDecoder.withJwkSetUri(jwks).build()
                : NimbusJwtDecoder.withIssuerLocation(issuer).build();

        decoder.setJwtValidator(buildJwtValidator(issuer, audience));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> buildJwtValidator(String issuer, String audienceProperty) {
        if (!StringUtils.hasText(audienceProperty)) {
            throw new IllegalStateException("JWT audience must be configured when JWT validation is enabled");
        }

        OAuth2TokenValidator<Jwt> baseValidator = StringUtils.hasText(issuer)
                ? JwtValidators.createDefaultWithIssuer(issuer)
                : JwtValidators.createDefault();

        Set<String> audiences = Arrays.stream(audienceProperty.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        if (audiences.isEmpty()) {
            throw new IllegalStateException("JWT audience must not be empty");
        }

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audiences);
        return new DelegatingOAuth2TokenValidator<>(baseValidator, audienceValidator);
    }

    private static class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final Set<String> allowedAudiences;

        AudienceValidator(Set<String> allowedAudiences) {
            this.allowedAudiences = allowedAudiences;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            if (token.getAudience() != null && token.getAudience().stream().anyMatch(allowedAudiences::contains)) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "The required audience is missing", null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}
