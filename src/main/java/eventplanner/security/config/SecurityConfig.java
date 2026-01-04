package eventplanner.security.config;

import eventplanner.security.filters.RbacContextFilter;
import eventplanner.security.filters.SecurityHeadersFilter;
import eventplanner.security.filters.ServiceApiKeyFilter;
import eventplanner.security.auth.jwt.CognitoJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;

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

    public SecurityConfig(SecurityHeadersFilter securityHeadersFilter,
                          RbacContextFilter rbacContextFilter,
                          ServiceApiKeyFilter serviceApiKeyFilter,
                          CognitoJwtAuthenticationConverter jwtAuthenticationConverter,
                          org.springframework.core.env.Environment environment) {
        this.securityHeadersFilter = securityHeadersFilter;
        this.rbacContextFilter = rbacContextFilter;
        this.serviceApiKeyFilter = serviceApiKeyFilter;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.issuerUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "");
        this.jwkSetUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", "");
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
            .formLogin(formLogin -> formLogin.disable());

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
    public JwtDecoder jwtDecoder(org.springframework.core.env.Environment environment) {
        String issuer = environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "");
        String jwks = environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", "");

        if (StringUtils.hasText(jwks)) {
            return NimbusJwtDecoder.withJwkSetUri(jwks).build();
        }
        if (StringUtils.hasText(issuer)) {
            return JwtDecoders.fromIssuerLocation(issuer);
        }
        // Dummy decoder to satisfy bean requirements when JWT validation is not configured locally.
        return token -> { throw new JwtException("JWT validation is disabled (no issuer configured)"); };
    }
}
