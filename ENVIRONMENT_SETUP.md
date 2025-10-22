# Environment Setup Guide

## Overview
This application requires all configuration values to be provided via environment variables. No default values are provided in `application.yml` for security reasons.

## Quick Start

1. **Copy the template:**
   ```bash
   cp env.template .env
   ```

2. **Fill in your values:**
   Edit `.env` with your actual configuration values

3. **Start the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

## Required Environment Variables

### 🔐 **Critical Security Variables**
These MUST be configured before running the application:

```bash
# JWT Secret (32+ characters)
JWT_SECRET=your_super_secure_jwt_secret_key_here_minimum_32_characters

# Database Password
DB_PASSWORD=your_secure_password_here

# Redis Password (if using authentication)
SPRING_REDIS_PASSWORD=your_redis_password_here
```

### 🗄️ **Database Configuration**
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=eventplanner
DB_USERNAME=postgres
DB_PASSWORD=your_secure_password_here
```

### 🔴 **Redis Configuration**
```bash
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=your_redis_password_here
```

### 🤖 **External AI Services**
```bash
# OpenAI API
OPENAI_API_KEY=your_openai_api_key_here

# Pinecone Vector Database
PINECONE_API_KEY=your_pinecone_api_key_here
PINECONE_ENVIRONMENT=us-east-1-aws
PINECONE_PROJECT=event-planner
```

### 📧 **Email Service**
```bash
SENDGRID_API_KEY=your_sendgrid_api_key_here
SENDGRID_FROM_EMAIL=noreply@eventplanner.com
```

### 🗺️ **External APIs**
```bash
GOOGLE_MAPS_API_KEY=your_google_maps_api_key_here
OPENWEATHER_API_KEY=your_openweather_api_key_here
```

### 🔗 **External Services**
```bash
# Shade Assistant (Python FastAPI Service)
SHADE_ASSISTANT_URL=http://localhost:8000

# Weather Service
WEATHER_BASE_URL=http://localhost:8089
```

## Development Setup

### 1. Database Setup
```bash
# Start PostgreSQL
docker run --name postgres-eventplanner \
  -e POSTGRES_DB=eventplanner \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=dev_password_123 \
  -p 5432:5432 \
  -d postgres:15
```

### 2. Redis Setup
```bash
# Start Redis
docker run --name redis-eventplanner \
  -p 6379:6379 \
  -d redis:7-alpine
```

### 3. Environment Variables for Development
```bash
# Copy template
cp env.template .env

# Edit with development values
DB_PASSWORD=dev_password_123
JWT_SECRET=dev_jwt_secret_key_32_chars_minimum
SPRING_REDIS_PASSWORD=
OPENAI_API_KEY=sk-your-openai-key-here
# ... fill in other values
```

## Production Setup

### 1. Security Requirements
- Use strong, unique passwords (20+ characters)
- Generate cryptographically secure JWT secrets
- Use environment-specific values
- Never commit `.env` files to version control
- Use secrets management (AWS Secrets Manager, HashiCorp Vault, etc.)

### 2. Environment-Specific Configuration
```bash
# Production database
DB_HOST=your-production-db-host
DB_PASSWORD=your-production-password

# Production Redis
SPRING_REDIS_HOST=your-production-redis-host
SPRING_REDIS_PASSWORD=your-production-redis-password

# Production CORS
APP_CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://app.yourdomain.com
```

### 3. External Services
Ensure all external services are properly configured:
- OpenAI API with appropriate rate limits
- Pinecone with production indexes
- SendGrid with verified sender domains
- Google Maps API with domain restrictions

## Validation

### Check Required Variables
The application will fail to start if critical variables are missing:

```bash
# Test configuration
./mvnw spring-boot:run --spring.profiles.active=test
```

### Common Issues

1. **JWT Secret Too Short**
   ```
   Error: JWT secret must be at least 32 characters long
   ```
   Solution: Use a longer, more secure secret

2. **Database Connection Failed**
   ```
   Error: Connection refused
   ```
   Solution: Check DB_HOST, DB_PORT, DB_USERNAME, DB_PASSWORD

3. **Redis Connection Failed**
   ```
   Error: Redis connection failed
   ```
   Solution: Check SPRING_REDIS_HOST, SPRING_REDIS_PORT, SPRING_REDIS_PASSWORD

## Security Best Practices

1. **Never commit `.env` files**
2. **Use different secrets for each environment**
3. **Rotate secrets regularly**
4. **Use secrets management in production**
5. **Restrict API key permissions**
6. **Monitor for exposed secrets**

## Troubleshooting

### Application Won't Start
1. Check all required environment variables are set
2. Verify database and Redis connections
3. Check JWT secret length (32+ characters)
4. Verify external service URLs are accessible

### External Service Errors
1. Verify API keys are valid and have proper permissions
2. Check rate limits on external APIs
3. Ensure external services are running and accessible
4. Check network connectivity

### Performance Issues
1. Adjust database connection pool settings
2. Configure Redis connection pool
3. Set appropriate cache sizes
4. Monitor external API rate limits
