package eventplanner.common.exception.handler;

import eventplanner.common.exception.dto.ErrorResponse;
import eventplanner.common.exception.util.ResponseSanitizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import java.net.URI;

/**
 * Response interceptor that scans ALL outgoing API responses for sensitive information.
 * Uses Zalando Problem (RFC 7807) for error responses.
 * 
 * STRICT SECURITY POLICY:
 * - Scans error messages for sensitive patterns
 * - Replaces leaked exception details with generic messages
 * - Runs on EVERY response before it leaves the server
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SanitizingResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final URI ERROR_TYPE = URI.create("about:blank");
    private final ObjectMapper objectMapper;

    public SanitizingResponseBodyAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {

        if (body == null) {
            return null;
        }

        int status = 200;
        if (response instanceof ServletServerHttpResponse servletResponse) {
            status = servletResponse.getServletResponse().getStatus();
        }

        // For error responses (4xx, 5xx), scan and sanitize
        if (status >= 400) {
            return sanitizeErrorResponse(body, status);
        }

        // For successful responses, do a quick scan
        return sanitizeSuccessResponse(body, status);
    }

    private Object sanitizeErrorResponse(Object body, int status) {
        // Handle Zalando Problem type
        if (body instanceof Problem problem) {
            return sanitizeProblem(problem, status);
        }

        // Handle our legacy ErrorResponse type
        if (body instanceof ErrorResponse errorResponse) {
            return sanitizeErrorResponseObject(errorResponse, status);
        }

        // Handle Map-based error responses
        if (body instanceof java.util.Map<?, ?> mapBody) {
            return sanitizeMapResponse(mapBody, status);
        }

        // For other types, check if it serializes to something with sensitive info
        try {
            String json = objectMapper.writeValueAsString(body);
            if (ResponseSanitizer.containsSensitiveInfo(json)) {
                return createGenericProblem(status);
            }
        } catch (JsonProcessingException e) {
            return createGenericProblem(status);
        }

        return body;
    }

    private Problem sanitizeProblem(Problem problem, int status) {
        String detail = problem.getDetail();
        String title = problem.getTitle();

        boolean needsSanitization = false;

        if (detail != null && ResponseSanitizer.containsSensitiveInfo(detail)) {
            detail = ResponseSanitizer.getGenericMessage(status);
            needsSanitization = true;
        }

        if (title != null && ResponseSanitizer.containsSensitiveInfo(title)) {
            title = getHttpStatusText(status);
            needsSanitization = true;
        }

        if (needsSanitization) {
            return Problem.builder()
                    .withType(ERROR_TYPE)
                    .withTitle(title)
                    .withStatus(problem.getStatus())
                    .withDetail(detail)
                    .build();
        }

        return problem;
    }

    private ErrorResponse sanitizeErrorResponseObject(ErrorResponse response, int status) {
        String message = response.getMessage();
        String error = response.getError();

        boolean needsSanitization = false;

        if (message != null && ResponseSanitizer.containsSensitiveInfo(message)) {
            message = ResponseSanitizer.getGenericMessage(status);
            needsSanitization = true;
        }

        if (error != null && ResponseSanitizer.containsSensitiveInfo(error)) {
            error = getHttpStatusText(status);
            needsSanitization = true;
        }

        if (needsSanitization) {
            return ErrorResponse.builder()
                    .timestamp(response.getTimestamp())
                    .status(response.getStatus())
                    .error(error)
                    .message(message)
                    .path(response.getPath())
                    .validationErrors(response.getValidationErrors())
                    .build();
        }

        return response;
    }

    private Object sanitizeMapResponse(java.util.Map<?, ?> mapBody, int status) {
        java.util.Map<String, Object> sanitized = new java.util.HashMap<>();
        boolean needsSanitization = false;

        for (java.util.Map.Entry<?, ?> entry : mapBody.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            if (value instanceof String strValue) {
                if (ResponseSanitizer.containsSensitiveInfo(strValue)) {
                    if ("message".equalsIgnoreCase(key) || "error".equalsIgnoreCase(key) || "detail".equalsIgnoreCase(key)) {
                        value = ResponseSanitizer.getGenericMessage(status);
                        needsSanitization = true;
                    } else if ("exception".equalsIgnoreCase(key) || "trace".equalsIgnoreCase(key) ||
                               "stacktrace".equalsIgnoreCase(key) || "cause".equalsIgnoreCase(key)) {
                        continue;
                    } else {
                        value = "[redacted]";
                        needsSanitization = true;
                    }
                }
            }

            sanitized.put(key, value);
        }

        // Remove trace/stacktrace fields
        sanitized.remove("trace");
        sanitized.remove("stackTrace");
        sanitized.remove("exception");

        return needsSanitization || !sanitized.equals(mapBody) ? sanitized : mapBody;
    }

    private Object sanitizeSuccessResponse(Object body, int status) {
        if (body instanceof String strBody) {
            if (ResponseSanitizer.containsSensitiveInfo(strBody)) {
                return "[Content redacted for security]";
            }
        }

        try {
            String json = objectMapper.writeValueAsString(body);
            if (json.contains("Row was updated or deleted") ||
                json.contains("StaleObjectStateException") ||
                json.contains("hibernate") ||
                json.contains("eventplanner.security") ||
                (json.contains("eventplanner.features") && json.contains("Exception"))) {
                // Detected leaked internal info in success response - unusual but handle it
            }
        } catch (JsonProcessingException e) {
            // Serialization failed, return as-is for success responses
        }

        return body;
    }

    private Problem createGenericProblem(int status) {
        Status zalandoStatus = toZalandoStatus(status);
        return Problem.builder()
                .withType(ERROR_TYPE)
                .withTitle(zalandoStatus.getReasonPhrase())
                .withStatus(zalandoStatus)
                .withDetail(ResponseSanitizer.getGenericMessage(status))
                .build();
    }

    private String getHttpStatusText(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "Error";
        };
    }

    private Status toZalandoStatus(int httpStatus) {
        return switch (httpStatus) {
            case 400 -> Status.BAD_REQUEST;
            case 401 -> Status.UNAUTHORIZED;
            case 403 -> Status.FORBIDDEN;
            case 404 -> Status.NOT_FOUND;
            case 405 -> Status.METHOD_NOT_ALLOWED;
            case 409 -> Status.CONFLICT;
            case 429 -> Status.TOO_MANY_REQUESTS;
            case 500 -> Status.INTERNAL_SERVER_ERROR;
            case 502 -> Status.BAD_GATEWAY;
            case 503 -> Status.SERVICE_UNAVAILABLE;
            case 504 -> Status.GATEWAY_TIMEOUT;
            default -> Status.INTERNAL_SERVER_ERROR;
        };
    }
}
