package eventplanner.security.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class ServiceApiKeyFilter extends OncePerRequestFilter {

    @Value("${service.auth.api-key:}")
    private String expectedApiKey;

    @Value("${service.auth.enabled:true}")
    private boolean serviceAuthEnabled;

    private final ObjectMapper objectMapper;

    public ServiceApiKeyFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // If service auth is disabled, skip validation
        if (!serviceAuthEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-Key");

        // If no API key is present, continue (regular mobile/user request)
        if (!StringUtils.hasText(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        // If API key is present, it must be valid
        if (!StringUtils.hasText(expectedApiKey) || !apiKey.equals(expectedApiKey)) {
            sendForbiddenResponse(response, "Invalid service API key", request.getRequestURI());
            return;
        }

        // API key is valid, continue
        filterChain.doFilter(request, response);
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message, String path) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
                "status", HttpServletResponse.SC_FORBIDDEN,
                "error", "Forbidden",
                "message", message,
                "path", path
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
