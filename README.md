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

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+
- RabbitMQ 3+

### Local Development

1. **Clone and setup environment:**
   ```bash
   cp env.template .env
   # Edit .env with your API keys
   ```

2. **Start infrastructure services:**
   ```bash
   docker-compose up -d postgres redis
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
   - RabbitMQ Management: http://localhost:15672 (guest/guest)
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
TWILIO_ACCOUNT_SID=your-twilio-sid
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
- Role-based access control (RBAC)
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
