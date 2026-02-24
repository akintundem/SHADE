package eventplanner.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import eventplanner.common.exception.util.ProblemBuilder;
import eventplanner.security.auth.jwt.OidcJwtAuthenticationConverter;
import eventplanner.security.filters.RbacContextFilter;
import eventplanner.security.filters.SecurityHeadersFilter;
import eventplanner.security.filters.ServiceApiKeyFilter;
import eventplanner.security.filters.TokenRevocationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
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
import org.zalando.problem.Problem;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    private final SecurityHeadersFilter securityHeadersFilter;
    private final RbacContextFilter rbacContextFilter;
    private final ServiceApiKeyFilter serviceApiKeyFilter;
    private final TokenRevocationFilter tokenRevocationFilter;
    private final OidcJwtAuthenticationConverter jwtAuthenticationConverter;
    private final ObjectMapper objectMapper;
    private final ResourceServerJwtProperties resourceServerJwtProperties;

    public WebSecurityConfig(SecurityHeadersFilter securityHeadersFilter,
                          RbacContextFilter rbacContextFilter,
                          ServiceApiKeyFilter serviceApiKeyFilter,
                          TokenRevocationFilter tokenRevocationFilter,
                          OidcJwtAuthenticationConverter jwtAuthenticationConverter,
                          ObjectMapper objectMapper,
                          ResourceServerJwtProperties resourceServerJwtProperties) {
        this.securityHeadersFilter = securityHeadersFilter;
        this.rbacContextFilter = rbacContextFilter;
        this.serviceApiKeyFilter = serviceApiKeyFilter;
        this.tokenRevocationFilter = tokenRevocationFilter;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.objectMapper = objectMapper;
        this.resourceServerJwtProperties = resourceServerJwtProperties;
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookieCustomizer(builder -> builder.httpOnly(true));
        return repo;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Defence-in-depth CORS: primary enforcement is at the Kong gateway,
        // but we also configure restrictive defaults here so direct calls are
        // safe even if the gateway is bypassed.
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(java.util.List.of(
                "${CORS_ALLOWED_ORIGINS:http://localhost:3000}"
        ));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of(
                "Authorization", "Content-Type", "X-API-Key", "X-Requested-With"
        ));
        config.setExposedHeaders(java.util.List.of("X-Request-ID"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Defence-in-depth CORS: primary enforcement at Kong gateway;
            // Spring-level config is a restrictive fallback.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository())
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher("/api/**")
                )) // API uses JWT; CSRF cookie hardened with HttpOnly
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/metrics").authenticated()
                .requestMatchers("/error", "/favicon.ico", "/css/**", "/js/**", "/images/**").permitAll()
                // SECURITY: Swagger restricted to ROLE_ADMIN only; disabled in prod via SWAGGER_UI_ENABLED=false
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable())
            // Custom exception handling using Zalando Problem (RFC 7807) for consistent error responses
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    Problem problem = ProblemBuilder.unauthorized("Authentication required");
                    writeProblemResponse(response, problem, 401);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    Problem problem = ProblemBuilder.forbidden("Access denied");
                    writeProblemResponse(response, problem, 403);
                })
            );

        http.addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(serviceApiKeyFilter, SecurityHeadersFilter.class);
        http.addFilterAfter(rbacContextFilter, BearerTokenAuthenticationFilter.class);
        // TokenRevocationFilter runs after BearerTokenAuthenticationFilter so the JWT is already
        // parsed into the SecurityContext; we then check the JTI against the Redis blocklist.
        http.addFilterAfter(tokenRevocationFilter, BearerTokenAuthenticationFilter.class);

        String issuerUri = resourceServerJwtProperties.getIssuerUri();
        String jwkSetUri = resourceServerJwtProperties.getJwkSetUri();

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
        String issuer = resourceServerJwtProperties.getIssuerUri();
        String jwks = resourceServerJwtProperties.getJwkSetUri();
        String audience = resourceServerJwtProperties.getAudiences();

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

    /**
     * Write Problem response to HTTP response using ObjectMapper.
     * Ensures consistent error format with the rest of the application.
     */
    private void writeProblemResponse(jakarta.servlet.http.HttpServletResponse response, Problem problem, int status) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status);
        response.getWriter().write(objectMapper.writeValueAsString(problem));
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
            if (allowedAudiences.isEmpty()) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "The required audience is missing", null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}
