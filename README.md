# Event Planner Monolith

This is the monolithic version of the AI-powered Event Planner application, consolidating all microservices into a single deployable unit.













## Architecture

The monolith includes all the functionality from the original microservices:

- **Authentication Service** - User management, JWT tokens, organizations
- **Event Service** - Event creation, management, AI-powered planning
- **Vendor Service** - Vendor directory, sourcing, quotes
- **Attendee Service** - Guest management, RSVPs, invitations
- **Budget Service** - Budget tracking, line items, payments
- **Communication Service** - Email, SMS, WhatsApp notifications
- **Risk Service** - Risk monitoring, alerts, mitigation
- **Timeline Service** - Event timelines, scheduling, run-of-show
- **Payments Service** - Stripe integration, payment processing
- **Weather Service** - Weather forecasts, alerts

## Request-to-Response Flow

The application follows a comprehensive security and processing pipeline for every incoming request:

### 🔄 Complete Request Flow

```
1. Client Request
   ↓
2. Endpoint Validation
   ↓
3. Security Headers Filter
   ↓
4. Client Validation Filter
   ↓
5. Rate Limiting Filter
   ↓
6. JWT Authentication Filter
   ↓
7. Controller Layer
   ↓
8. Service Layer
   ↓
9. Repository Layer
    ↓
11. Database Operations
    ↓
12. Response Processing
    ↓
13. Client Response
```

### 📋 Detailed Flow Breakdown

#### **1. Client Request**
- **Web Client**: Browser sends HTTP request with CORS headers
- **Mobile Client**: Mobile app sends HTTP request with JWT token
- **API Client**: External service sends authenticated request

#### **2. Endpoint Validation**
- **Spring Security** validates the request matches configured endpoints
- **Public Endpoints**: `/api/v1/auth/login`, `/api/v1/auth/register`, `/health`
- **Protected Endpoints**: All other `/api/**` endpoints require authentication

#### **3. Security Headers Filter** (`SecurityHeadersFilter`)
- **Adds Security Headers**:
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `X-XSS-Protection: 1; mode=block`
  - `Strict-Transport-Security: max-age=31536000`
  - `Content-Security-Policy: default-src 'self'`

#### **4. Client Validation Filter** (`ClientValidationFilter`)
- **Validates Client ID** from `X-Client-ID` header
- **Checks Client Registration** in database
- **Anonymous Clients**: Uses default "anonymous" client for public endpoints
- **Registered Clients**: Validates against `ClientApplication` entity

#### **5. Rate Limiting Filter** (`RateLimitingFilter`)
- **Redis-based Rate Limiting**:
  - **Per-minute limits**: Based on client configuration
  - **Per-hour limits**: Based on client configuration
  - **Stricter limits** for authentication endpoints
- **Anonymous Limits**: 5/min, 20/hour for auth; 10/min, 100/hour for others
- **Registered Client Limits**: Configurable per client

#### **6. JWT Authentication Filter** (`JwtAuthenticationFilter`)
- **Extracts JWT Token** from `Authorization: Bearer <token>` header
- **Validates Token Signature** using configured secret
- **Checks Token Expiration** and refresh token validity
- **Loads User Details** including roles and permissions
- **Sets Security Context** with authenticated user

#### **7. Controller Layer**
- **Spring MVC Controllers** handle HTTP requests
- **Request Mapping**: Routes to appropriate controller method
- **Input Validation**: Validates request body and parameters
- **Response Mapping**: Converts service responses to HTTP responses

#### **8. Service Layer**
- **Business Logic**: Implements core application functionality
- **Transaction Management**: Handles database transactions
- **External API Integration**: Calls third-party services
- **Data Transformation**: Converts between DTOs and entities

#### **9. Repository Layer**
- **JPA Repositories**: Database access layer
- **Query Execution**: Runs SQL queries against PostgreSQL
- **Entity Management**: Handles entity lifecycle
- **Caching**: Redis caching for frequently accessed data

#### **10. Database Operations**
- **PostgreSQL Database**: Primary data storage
- **ACID Transactions**: Ensures data consistency
- **Connection Pooling**: Manages database connections
- **Query Optimization**: Optimized queries for performance

#### **11. Response Processing**
- **JSON Serialization**: Converts objects to JSON
- **CORS Headers**: Adds CORS headers for web clients
- **Security Headers**: Adds security headers to response
- **Error Handling**: Formats error responses

#### **13. Client Response**
- **HTTP Response**: Returns processed response to client
- **Status Codes**: Appropriate HTTP status codes
- **Response Headers**: Security and CORS headers
- **Response Body**: JSON data or error messages

### 🔐 Security Layers

#### **Authentication Flow**
```
1. Client sends credentials → /api/v1/auth/login
2. AuthService validates credentials
3. JWT token generated with user roles
4. Token returned to client
5. Client includes token in subsequent requests
```

#### **Authorization Flow**
```
1. Request includes JWT token
2. JWT Authentication Filter validates token
3. UserPrincipal loaded with roles/permissions
4. Authorization checks performed in service layer
5. Request allowed/denied based on ownership and roles
```

#### **CORS Flow**
```
1. Browser sends preflight OPTIONS request
2. CORS configuration validates origin
3. Appropriate CORS headers returned
4. Browser sends actual request
5. Response includes CORS headers
```

### 📊 Data Flow Examples

#### **Event Creation Flow**
```
POST /api/v1/events
1. Security filters validate request
2. EventController receives request
3. EventService creates event
4. EventRepository saves to database
5. Response returned with event details
```

#### **Budget Management Flow**
```
PUT /api/v1/budgets/{id}
1. Authorization checks ownership/permissions
2. BudgetController validates request
3. BudgetService updates budget
4. Database transaction commits
5. Updated budget returned
```

#### **Vendor Search Flow**
```
GET /api/v1/vendors?search=catering
1. Rate limiting applied
2. VendorController processes search
3. VendorService queries database
4. Results cached in Redis
5. Paginated results returned
```

### 🚀 Performance Optimizations

- **Redis Caching**: Frequently accessed data cached
- **Connection Pooling**: Database connections reused
- **Rate Limiting**: Prevents abuse and ensures fair usage
- **CORS Preflight Caching**: 1-hour cache for OPTIONS requests
- **JWT Stateless**: No server-side session storage
- **Database Indexing**: Optimized queries with proper indexes

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+

### Local Development

1. **Clone and setup environment:**
   ```bash
   cp env.template .env
   # Edit .env with your API keys
   ```

2. **Start infrastructure services:**
   ```bash
   docker-compose up -d postgres redis mongodb
   ```
   > Alternatively, install PostgreSQL locally and ensure it is running on `localhost:5432` with credentials matching `.env`.

3. **Start the LangChain assistant (Python):**
   ```bash
   cd shade-assistant
   python3 -m venv .venv
   source .venv/bin/activate
   pip install -r requirements.txt
   uvicorn app.main:app --port 9000 --reload
   ```

4. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

5. **Access the application:**
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui
   - Health Check: http://localhost:8080/actuator/health

### Docker Deployment

1. **Build and run everything:**
   ```bash
   docker-compose up --build
   ```

2. **Access services:**
   - Application: http://localhost:8080
   - MailHog: http://localhost:8025

## API Endpoints

### Authentication
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh` - Refresh JWT token

### Events
- `GET /api/v1/events` - List events
- `POST /api/v1/events` - Create event
- `GET /api/v1/events/{id}` - Get event details
- `PUT /api/v1/events/{id}` - Update event
- `DELETE /api/v1/events/{id}` - Delete event

### Vendors
- `GET /api/v1/vendors` - List vendors
- `POST /api/v1/vendors` - Create vendor
- `GET /api/v1/vendors/{id}` - Get vendor details

### Attendees
- `GET /api/v1/attendees` - List attendees
- `POST /api/v1/attendees` - Add attendee
- `GET /api/v1/attendees/{id}` - Get attendee details

### Budget
- `GET /api/v1/budgets` - List budgets
- `POST /api/v1/budgets` - Create budget
- `GET /api/v1/budgets/{id}` - Get budget details

### Communications
- `POST /api/v1/comms/send-email` - Send email
- `POST /api/v1/comms/send-sms` - Send SMS

### Risk Management
- `GET /api/v1/risks` - List risks
- `POST /api/v1/risks` - Create risk assessment

### Timeline
- `GET /api/v1/timelines` - List timelines
- `POST /api/v1/timelines` - Create timeline

### Payments
- `POST /api/v1/payments/create-intent` - Create payment intent
- `POST /api/v1/payments/confirm` - Confirm payment

### Weather
- `GET /api/v1/weather/forecast` - Get weather forecast

## Configuration

The application uses Spring Boot's configuration system. Key configuration files:

- `application.yml` - Main configuration
- `env.template` - Environment variables template

### Required Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=eventplanner
DB_USERNAME=postgres
DB_PASSWORD=postgres

# JWT
JWT_SECRET=your-secret-key

# AI Services
OPENAI_API_KEY=your-openai-key
PINECONE_API_KEY=your-pinecone-key

# External APIs
STRIPE_API_KEY=your-stripe-key
SENDGRID_API_KEY=your-sendgrid-key
GOOGLE_MAPS_API_KEY=your-google-maps-key
```

## Database Schema

The application uses PostgreSQL with Flyway for database migrations. Migration files are located in `src/main/resources/db/migration/`.

## Development

### Project Structure

```
src/main/java/ai/eventplanner/
├── EventPlannerApplication.java    # Main application class
├── config/                         # Configuration classes
├── auth/                          # Authentication module
├── event/                         # Event management module
├── vendor/                        # Vendor management module
├── attendee/                      # Attendee management module
├── budget/                        # Budget management module
├── comms/                         # Communication module
├── risk/                          # Risk management module
├── timeline/                      # Timeline management module
├── payments/                      # Payment processing module
├── weather/                       # Weather service module
└── common/                        # Shared utilities and DTOs
```

### Building

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package application
mvn package

# Run application
mvn spring-boot:run
```

### Testing

#### Unit Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuthServiceTest

# Run with coverage
mvn test jacoco:report
```

#### API Testing with Postman
```bash
# Run all Postman collections
./test.sh

# Run specific collection
newman run "Postman Collections/Event_Planner_Auth_Service_Testing.postman_collection.json" -e test-environment.json

# View test results
ls test-results/
```

#### Test Environment
- **Test Environment**: `test-environment.json` - Configured for monolith on port 8080
- **Postman Collections**: All service collections available in `Postman Collections/`
- **Test Results**: Generated in `test-results/` directory with detailed markdown reports

## Monitoring

The application includes Spring Boot Actuator for monitoring:

- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Environment: `/actuator/env`
- Info: `/actuator/info`

## Security

- JWT-based authentication
- Role-based authorization
- CORS configuration
- Input validation
- SQL injection protection

## Differences from Microservices

This monolithic version consolidates all services into a single application:

1. **Single Database**: All services share the same PostgreSQL database
2. **Shared Configuration**: Single `application.yml` file
3. **Internal Communication**: Direct method calls instead of HTTP
4. **Single Deployment**: One JAR file instead of multiple services
5. **Simplified Networking**: No service discovery or API gateway needed

## Migration from Microservices

To migrate from the microservices version:

1. Copy this monolith structure
2. Update your CI/CD pipeline to build a single JAR
3. Update your deployment scripts
4. Consolidate your monitoring and logging
5. Update your load balancer configuration

## Support

For issues and questions, please refer to the main Event Planner documentation or contact the development team.
