package eventplanner.security.config;

import eventplanner.security.filters.GatewayIdentityFilter;
import eventplanner.security.filters.RbacContextFilter;
import eventplanner.security.filters.SecurityHeadersFilter;
import eventplanner.security.filters.ServiceApiKeyFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final SecurityHeadersFilter securityHeadersFilter;
    private final RbacContextFilter rbacContextFilter;
    private final ServiceApiKeyFilter serviceApiKeyFilter;
    private final GatewayIdentityFilter gatewayIdentityFilter;

    public SecurityConfig(SecurityHeadersFilter securityHeadersFilter,
                          RbacContextFilter rbacContextFilter,
                          ServiceApiKeyFilter serviceApiKeyFilter,
                          GatewayIdentityFilter gatewayIdentityFilter) {
        this.securityHeadersFilter = securityHeadersFilter;
        this.rbacContextFilter = rbacContextFilter;
        this.serviceApiKeyFilter = serviceApiKeyFilter;
        this.gatewayIdentityFilter = gatewayIdentityFilter;
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

        // Execution order (outermost -> innermost):
        // 1. SecurityHeadersFilter
        // 2. ServiceApiKeyFilter
        // 3. GatewayIdentityFilter (trusts gateway-authenticated user headers)
        // 4. RbacContextFilter
        // 5. UsernamePasswordAuthenticationFilter (Spring Security)
        http.addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(serviceApiKeyFilter, SecurityHeadersFilter.class);
        http.addFilterAfter(gatewayIdentityFilter, ServiceApiKeyFilter.class);
        http.addFilterAfter(rbacContextFilter, GatewayIdentityFilter.class);

        return http.build();
    }
}
