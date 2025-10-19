.PHONY: help build run start stop restart logs clean test postman

# Default target - rcd un the application
.DEFAULT_GOAL := run

help:
	@echo "Event Planner Monolith - Make Targets"
	@echo ""
	@echo "  make          - Build and run the monolithic application (default)"
	@echo "  make run      - Build and run the monolithic application"
	@echo "  make build    - Build the application with Maven"
	@echo "  make start    - Start the application (if already built)"
	@echo "  make stop     - Stop the running application"
	@echo "  make restart  - Stop, rebuild, and restart the application"
	@echo "  make logs     - Show application logs"
	@echo "  make clean    - Clean build artifacts and stop application"
	@echo "  make test     - Spin up dependencies, launch the app, and run Postman smoke tests"
	@echo "  make postman  - Alias for make test"
	@echo ""

# Default target - build and run
run: build start

build:
	@echo "Building monolithic application..."
	mvn clean package -DskipTests
	@echo "Build completed!"

start:
	@echo "Starting Event Planner Monolith..."
	@echo "Application will be available at: http://localhost:8080"
	@echo "Auth health check: http://localhost:8080/api/v1/auth/health"
	@echo "Press Ctrl+C to stop the application"
	@echo ""
	@if [ -f .env ]; then \
		echo "Loading environment variables from .env file..."; \
		export $$(grep -v '^#' .env | grep -v '^$$' | xargs) && java -jar target/event-planner-monolith-1.0.0.jar; \
	else \
		echo "No .env file found, starting without environment variables..."; \
		java -jar target/event-planner-monolith-1.0.0.jar; \
	fi

stop:
	@echo "Stopping application..."
	@pkill -f "java -jar target/event-planner-monolith-1.0.0.jar" || true
	@echo "Application stopped!"

restart: stop build start

logs:
	@echo "Showing application logs..."
	@tail -f logs/application.log 2>/dev/null || echo "No log file found. Application may not be running."

clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	@echo "Stopping any running instances..."
	@pkill -f "java -jar target/event-planner-monolith-1.0.0.jar" || true
	@echo "Clean completed!"

test:
	@./scripts/test.sh

postman: test
