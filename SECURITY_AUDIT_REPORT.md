# Security Audit Report - Secrets & Vulnerabilities

**Date:** $(date)
**Project:** sade-mono
**Scope:** All files in `src/` directory

## Executive Summary

This security audit identified **3 CRITICAL** and **2 MEDIUM** security issues related to secrets management and potential vulnerabilities. While most secrets are properly externalized via environment variables, there are several critical issues that need immediate attention.

---

## 🔴 CRITICAL ISSUES

### 1. Hardcoded JWT Secret in docker-compose.yml (CRITICAL)

**Location:** `docker-compose.yml:98`

**Issue:**
```yaml
JWT_SECRET: ${JWT_SECRET:-***REMOVED***}
```

**Risk:** 
- A default JWT secret is hardcoded in the docker-compose file
- If `JWT_SECRET` environment variable is not set, the application will use this predictable secret
- This allows attackers to forge JWT tokens and impersonate users
- The secret is visible in version control

**Recommendation:**
- Remove the default value entirely
- Require `JWT_SECRET` to be explicitly set
- Change: `JWT_SECRET: ${JWT_SECRET}` (no default)
- Add validation at startup to ensure it's set and meets minimum length requirements (already implemented in code)

---

### 2. Hardcoded Internal Assistant Secret in docker-compose.yml (CRITICAL)

**Location:** `docker-compose.yml:107`

**Issue:**
```yaml
INTERNAL_ASSISTANT_SECRET: ${INTERNAL_ASSISTANT_SECRET:-local_assistant_secret}
```

**Risk:**
- Default secret `local_assistant_secret` is predictable and weak
- Used for internal service authentication between Java and Python services
- If not overridden, allows unauthorized access to internal endpoints
- Visible in version control

**Recommendation:**
- Remove the default value
- Change: `INTERNAL_ASSISTANT_SECRET: ${INTERNAL_ASSISTANT_SECRET}` (no default)
- Ensure the secret is set via environment variables or secrets management system

---

### 3. Management Endpoint Exposes Environment Variables (CRITICAL)

**Location:** `src/main/resources/application.yml:192`

**Issue:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: ${MANAGEMENT_ENDPOINTS:health,info,metrics,env}
```

**Risk:**
- The `/actuator/env` endpoint is exposed by default
- This endpoint exposes ALL environment variables including:
  - `JWT_SECRET`
  - `DB_PASSWORD`
  - `OPENAI_API_KEY`
  - `PINECONE_API_KEY`
  - `RESEND_API_KEY`
  - `GOOGLE_MAPS_API_KEY`
  - `OPENWEATHER_API_KEY`
  - `FIREBASE_SERVICE_ACCOUNT_KEY_PATH`
  - And all other secrets
- While `/actuator/env` requires authentication (per SecurityConfig), if authentication is bypassed or misconfigured, all secrets are exposed
- Even with authentication, exposing this endpoint increases attack surface

**Recommendation:**
- Remove `env` from the default list
- Change to: `include: ${MANAGEMENT_ENDPOINTS:health,info,metrics}`
- If `env` endpoint is needed for debugging, ensure it's:
  - Only enabled in development environments
  - Protected by strong authentication
  - Restricted to specific IPs/networks
  - Never exposed in production

---

## 🟡 MEDIUM ISSUES

### 4. Default Database Credentials in docker-compose.yml (MEDIUM)

**Location:** `docker-compose.yml:81-82`

**Issue:**
```yaml
DB_USERNAME: ${DB_USERNAME:-postgres}
DB_PASSWORD: ${DB_PASSWORD:-postgres}
```

**Risk:**
- Default database credentials are weak and predictable
- If environment variables are not set, the application uses `postgres/postgres`
- While this is acceptable for local development, it's dangerous if deployed without proper configuration

**Recommendation:**
- Keep defaults for local development but add clear warnings
- Add startup validation to warn if production-like environment uses default credentials
- Document that these MUST be changed in production

---

### 5. Potential Secret Logging in ResendConfig (MEDIUM)

**Location:** `src/main/java/eventplanner/common/communication/config/ResendConfig.java:24`

**Issue:**
```java
log.info("Resend API key is configured (length: {} characters)", resendApiKey.length());
```

**Risk:**
- While only logging the length (not the actual key), this is still information disclosure
- Logs could be accessed by unauthorized users
- Reveals that an API key is configured

**Recommendation:**
- Consider removing this log or changing to DEBUG level
- If logging is necessary, ensure logs are properly secured and access-controlled
- Consider using a more generic message without revealing configuration details

---

## ✅ POSITIVE FINDINGS

### Good Security Practices Found:

1. **No Hardcoded Secrets in Source Code**
   - All secrets are properly externalized via environment variables
   - No API keys, passwords, or tokens found hardcoded in Java source files

2. **Proper Secret Validation**
   - `JwtValidationUtil.java` validates JWT secret length (minimum 32 characters)
   - `TokenService.java` validates JWT secret is not empty
   - `ResendConfig.java` validates API key is configured before use

3. **Secure Configuration Loading**
   - Firebase credentials loaded from file path (not hardcoded)
   - All external API keys loaded from environment variables
   - Proper use of Spring's `@Value` annotation with environment variable substitution

4. **No Direct System.getenv() Usage**
   - Only one instance found in `ShadeAssistantService.java` which is acceptable for internal service auth
   - Most configuration uses Spring's dependency injection

5. **Management Endpoints Protected**
   - `/actuator/metrics` requires authentication
   - `/actuator/health` and `/actuator/info` are public (acceptable)

---

## 📋 RECOMMENDATIONS SUMMARY

### Immediate Actions Required:

1. **Remove default JWT_SECRET** from docker-compose.yml
2. **Remove default INTERNAL_ASSISTANT_SECRET** from docker-compose.yml
3. **Remove `env` from management endpoints** default exposure list
4. **Add startup validation** to ensure critical secrets are set
5. **Review log levels** for secret-related information

### Best Practices to Implement:

1. **Use Secrets Management**
   - Consider using AWS Secrets Manager, HashiCorp Vault, or similar
   - Never commit secrets to version control

2. **Environment-Specific Configuration**
   - Use Spring profiles to separate dev/staging/prod configurations
   - Ensure production profiles never have defaults for secrets

3. **Regular Secret Rotation**
   - Implement a process for rotating JWT secrets and API keys
   - Document rotation procedures

4. **Security Scanning**
   - Add pre-commit hooks to detect secrets
   - Use tools like `git-secrets`, `truffleHog`, or `gitleaks`
   - Integrate into CI/CD pipeline

5. **Access Control**
   - Ensure management endpoints are properly secured
   - Use network-level restrictions where possible
   - Implement audit logging for access to sensitive endpoints

---

## 🔍 FILES REVIEWED

- ✅ `src/main/resources/application.yml` - Configuration file
- ✅ `docker-compose.yml` - Docker configuration
- ✅ All Java files in `src/main/java/` - Source code
- ✅ Configuration classes (FirebaseConfig, ResendConfig, etc.)
- ✅ Security-related classes (TokenService, JwtValidationUtil, etc.)
- ✅ Service classes using external APIs

---

## 📝 NOTES

- The codebase generally follows good security practices
- Most issues are configuration-related rather than code-related
- The critical issues are easily fixable and don't require code changes
- No evidence of secrets being logged or exposed in error messages
- Proper validation exists for critical secrets

---

**Report Generated:** $(date)
**Next Review:** Recommended after fixes are implemented

