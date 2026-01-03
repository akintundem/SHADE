.PHONY: help build run start stop restart logs clean test postman \
	compose-up compose-up-build compose-down compose-build compose-rebuild compose-rebuild-up \
	compose-logs compose-logs-tail compose-ps compose-restart compose-clean compose-recreate \
	service-rebuild service-recreate service-logs service-stop service-start service-restart \
	dev-up dev-down dev-logs dev-restart \
	compose-up-dev compose-up-prod compose-down-dev compose-down-prod

# Default target
.DEFAULT_GOAL := help

# Docker Compose file
COMPOSE_FILE := docker-compose.yml

# Auto-detect environment based on git branch
GIT_BRANCH := $(shell git branch --show-current 2>/dev/null || echo "main")
ENV_FILE := $(if $(filter development,$(GIT_BRANCH)),.env.dev,.env.prod)
ENV_NAME := $(if $(filter development,$(GIT_BRANCH)),DEV,PROD)

# Colors for output
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

help:
	@echo "$(GREEN)Event Planner Monolith - Make Targets$(NC)"
	@echo ""
	@echo "$(YELLOW)Docker Compose Commands (with Hot-Reload):$(NC)"
	@echo "  make compose-up          - Start all services (code changes auto-reload!)"
	@echo "  make compose-up-build    - Build (with cache) then start all services"
	@echo "  make compose-rebuild-up  - Rebuild (no cache) then start all services"
	@echo "  make compose-up-dev      - Start services with .env.dev file"
	@echo "  make compose-up-prod     - Start services with .env.prod file"
	@echo ""
	@echo "$(YELLOW)Note:$(NC) Hot-reload is enabled by default! Edit code and changes reflect immediately."
	@echo "  make compose-down        - Stop and remove all containers"
	@echo "  make compose-down-dev    - Stop and remove containers (dev environment)"
	@echo "  make compose-down-prod   - Stop and remove containers (prod environment)"
	@echo "  make compose-build       - Build all service images (doesn't start)"
	@echo "  make compose-rebuild     - Rebuild all service images (no cache, doesn't start)"
	@echo "  make compose-restart     - Restart all services"
	@echo "  make compose-logs        - Show logs from all services"
	@echo "  make compose-ps          - Show status of all services"
	@echo "  make compose-clean       - Stop containers and remove volumes"
	@echo "  make compose-recreate    - Recreate all containers"
	@echo ""
	@echo "$(YELLOW)Service Management:$(NC)"
	@echo "  make service-rebuild SERVICE=<name>  - Rebuild (no cache) then restart a specific service"
	@echo "  make service-recreate SERVICE=<name> - Recreate a specific service"
	@echo "  make service-logs SERVICE=<name>     - Show logs for a specific service"
	@echo "  make service-stop SERVICE=<name>    - Stop a specific service"
	@echo "  make service-start SERVICE=<name>   - Start a specific service"
	@echo "  make service-restart SERVICE=<name> - Restart a specific service"
	@echo ""
	@echo "$(YELLOW)Available Services:$(NC)"
	@echo "  - java-app"
	@echo "  - email-service"
	@echo "  - push-service"
	@echo "  - ai-service"
	@echo ""
	@echo "$(YELLOW)Legacy Commands:$(NC)"
	@echo "  make build              - Build the application with Maven"
	@echo "  make run                - Build and run the monolithic application (non-Docker)"
	@echo "  make start              - Start the application (non-Docker)"
	@echo "  make stop               - Stop the running application (non-Docker)"
	@echo "  make restart            - Stop, rebuild, and restart the application (non-Docker)"
	@echo "  make logs               - Show application logs (non-Docker)"
	@echo "  make clean              - Clean build artifacts (non-Docker)"
	@echo "  make test               - Run Postman smoke tests"
	@echo "  make postman            - Alias for make test"
	@echo ""
	@echo "$(YELLOW)Examples:$(NC)"
	@echo "  make compose-up                    # Start services (auto-detects branch: dev/prod)"
	@echo "  make compose-up-dev                # Start services with .env.dev (force dev)"
	@echo "  make compose-up-prod               # Start services with .env.prod (force prod)"
	@echo "  make compose-rebuild-up            # Rebuild (no cache) then start all"
	@echo "  make service-rebuild SERVICE=java-app  # Rebuild then restart Java app"
	@echo "  make service-logs SERVICE=java-app     # View Java app logs"
	@echo ""
	@echo "$(YELLOW)Environment Detection:$(NC)"
	@echo "  - On 'development' branch: automatically uses .env.dev"
	@echo "  - On 'main' branch: automatically uses .env.prod"

# ============================================================================
# Docker Compose Commands
# ============================================================================

compose-up:
	@echo "$(GREEN)Starting all services with Docker Compose...$(NC)"
	@echo "$(YELLOW)Detected branch: $(GIT_BRANCH) - Using $(ENV_FILE) ($(ENV_NAME))$(NC)"
	@if [ ! -f $(ENV_FILE) ]; then \
		echo "$(RED)ERROR: $(ENV_FILE) file not found!$(NC)"; \
		echo "$(YELLOW)Please create $(ENV_FILE) file with your credentials$(NC)"; \
		exit 1; \
	fi
	ENV_FILE=$(ENV_FILE) docker-compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) up -d
	@echo "$(GREEN)Services started with $(ENV_NAME) configuration!$(NC)"
	@echo "$(YELLOW)Java App: http://localhost:8080$(NC)"
	@echo "$(YELLOW)Push Service: http://localhost:${PUSH_SERVICE_PORT:-3100}$(NC)"
	@make compose-ps

compose-up-dev:
	@if [ ! -f .env.dev ]; then \
		echo "$(RED)ERROR: .env.dev file not found!$(NC)"; \
		echo "$(YELLOW)Please create .env.dev file with your credentials$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)Starting all services with Docker Compose (DEV environment)...$(NC)"
	ENV_FILE=.env.dev docker-compose -f $(COMPOSE_FILE) --env-file .env.dev up -d
	@echo "$(GREEN)Services started with DEV configuration!$(NC)"
	@echo "$(YELLOW)Java App: http://localhost:8080$(NC)"
	@echo "$(YELLOW)Push Service: http://localhost:${PUSH_SERVICE_PORT:-3100}$(NC)"
	@make compose-ps

compose-up-prod:
	@if [ ! -f .env.prod ]; then \
		echo "$(RED)ERROR: .env.prod file not found!$(NC)"; \
		echo "$(YELLOW)Please create .env.prod file with your credentials$(NC)"; \
		exit 1; \
	fi
	@echo "$(RED)WARNING: Starting PRODUCTION environment!$(NC)"
	@echo "$(YELLOW)Starting all services with Docker Compose (PROD environment)...$(NC)"
	ENV_FILE=.env.prod docker-compose -f $(COMPOSE_FILE) --env-file .env.prod up -d
	@echo "$(GREEN)Services started with PROD configuration!$(NC)"
	@echo "$(YELLOW)Java App: http://localhost:8080$(NC)"
	@echo "$(YELLOW)Push Service: http://localhost:${PUSH_SERVICE_PORT:-3100}$(NC)"
	@make compose-ps

compose-up-build:
	@echo "$(GREEN)Building and starting all services...$(NC)"
	@echo "$(YELLOW)Detected branch: $(GIT_BRANCH) - Using $(ENV_FILE) ($(ENV_NAME))$(NC)"
	@if [ ! -f $(ENV_FILE) ]; then \
		echo "$(RED)ERROR: $(ENV_FILE) file not found!$(NC)"; \
		echo "$(YELLOW)Please create $(ENV_FILE) file with your credentials$(NC)"; \
		exit 1; \
	fi
	ENV_FILE=$(ENV_FILE) docker-compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) up -d --build
	@echo "$(GREEN)Services built and started with $(ENV_NAME) configuration!$(NC)"
	@make compose-ps

compose-down:
	@echo "$(YELLOW)Stopping and removing all containers...$(NC)"
	@echo "$(YELLOW)Detected branch: $(GIT_BRANCH) - Using $(ENV_FILE) ($(ENV_NAME))$(NC)"
	@if [ -f $(ENV_FILE) ]; then \
		ENV_FILE=$(ENV_FILE) docker-compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) down; \
	else \
		docker-compose -f $(COMPOSE_FILE) down; \
	fi
	@echo "$(GREEN)All services stopped and removed!$(NC)"

compose-down-dev:
	@echo "$(YELLOW)Stopping and removing all containers (DEV environment)...$(NC)"
	@if [ -f .env.dev ]; then \
		ENV_FILE=.env.dev docker-compose -f $(COMPOSE_FILE) --env-file .env.dev down; \
	else \
		docker-compose -f $(COMPOSE_FILE) down; \
	fi
	@echo "$(GREEN)All services stopped and removed!$(NC)"

compose-down-prod:
	@echo "$(RED)Stopping and removing all containers (PROD environment)...$(NC)"
	@if [ -f .env.prod ]; then \
		ENV_FILE=.env.prod docker-compose -f $(COMPOSE_FILE) --env-file .env.prod down; \
	else \
		docker-compose -f $(COMPOSE_FILE) down; \
	fi
	@echo "$(GREEN)All services stopped and removed!$(NC)"

compose-build:
	@echo "$(GREEN)Building all service images...$(NC)"
	docker-compose -f $(COMPOSE_FILE) build
	@echo "$(GREEN)Build completed!$(NC)"

compose-rebuild:
	@echo "$(GREEN)Rebuilding all service images (no cache)...$(NC)"
	docker-compose -f $(COMPOSE_FILE) build --no-cache
	@echo "$(GREEN)Rebuild completed!$(NC)"

compose-rebuild-up:
	@echo "$(GREEN)Rebuilding all service images (no cache) then starting...$(NC)"
	@echo "$(YELLOW)Detected branch: $(GIT_BRANCH) - Using $(ENV_FILE) ($(ENV_NAME))$(NC)"
	@if [ ! -f $(ENV_FILE) ]; then \
		echo "$(RED)ERROR: $(ENV_FILE) file not found!$(NC)"; \
		echo "$(YELLOW)Please create $(ENV_FILE) file with your credentials$(NC)"; \
		exit 1; \
	fi
	docker-compose -f $(COMPOSE_FILE) build --no-cache
	ENV_FILE=$(ENV_FILE) docker-compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) up -d
	@echo "$(GREEN)All services rebuilt and started with $(ENV_NAME) configuration!$(NC)"
	@make compose-ps

compose-restart:
	@echo "$(YELLOW)Restarting all services...$(NC)"
	@echo "$(YELLOW)Detected branch: $(GIT_BRANCH) - Using $(ENV_FILE) ($(ENV_NAME))$(NC)"
	@if [ -f $(ENV_FILE) ]; then \
		ENV_FILE=$(ENV_FILE) docker-compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) restart; \
	else \
		docker-compose -f $(COMPOSE_FILE) restart; \
	fi
	@echo "$(GREEN)All services restarted!$(NC)"

compose-logs:
	@echo "$(GREEN)Showing logs from all services...$(NC)"
	docker-compose -f $(COMPOSE_FILE) logs -f

compose-logs-tail:
	@echo "$(GREEN)Showing last 100 lines of logs...$(NC)"
	docker-compose -f $(COMPOSE_FILE) logs --tail=100

compose-ps:
	@echo "$(GREEN)Service Status:$(NC)"
	@docker-compose -f $(COMPOSE_FILE) ps

compose-clean:
	@echo "$(RED)WARNING: This will stop containers and remove volumes!$(NC)"
	@echo "$(YELLOW)Stopping containers and removing volumes...$(NC)"
	docker-compose -f $(COMPOSE_FILE) down -v
	@echo "$(GREEN)Clean completed!$(NC)"

compose-recreate:
	@echo "$(GREEN)Recreating all containers...$(NC)"
	docker-compose -f $(COMPOSE_FILE) up -d --force-recreate
	@echo "$(GREEN)All containers recreated!$(NC)"

# ============================================================================
# Service-Specific Commands
# ============================================================================

service-rebuild:
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)ERROR: SERVICE parameter is required$(NC)"; \
		echo "Usage: make service-rebuild SERVICE=<service-name>"; \
		echo "Available services: java-app, email-service, push-service"; \
		exit 1; \
	fi
	@echo "$(GREEN)Rebuilding service: $(SERVICE)$(NC)"
	docker-compose -f $(COMPOSE_FILE) build --no-cache $(SERVICE)
	@echo "$(GREEN)Rebuilding completed! Restarting service...$(NC)"
	docker-compose -f $(COMPOSE_FILE) up -d --no-deps $(SERVICE)
	@echo "$(GREEN)Service $(SERVICE) rebuilt and restarted!$(NC)"

service-recreate:
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)ERROR: SERVICE parameter is required$(NC)"; \
		echo "Usage: make service-recreate SERVICE=<service-name>"; \
		echo "Available services: java-app, email-service, push-service"; \
		exit 1; \
	fi
	@echo "$(GREEN)Recreating service: $(SERVICE)$(NC)"
	docker-compose -f $(COMPOSE_FILE) up -d --force-recreate --no-deps $(SERVICE)
	@echo "$(GREEN)Service $(SERVICE) recreated!$(NC)"

service-logs:
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)ERROR: SERVICE parameter is required$(NC)"; \
		echo "Usage: make service-logs SERVICE=<service-name>"; \
		echo "Available services: java-app, email-service, push-service"; \
		exit 1; \
	fi
	@echo "$(GREEN)Showing logs for service: $(SERVICE)$(NC)"
	docker-compose -f $(COMPOSE_FILE) logs -f $(SERVICE)

service-stop:
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)ERROR: SERVICE parameter is required$(NC)"; \
		echo "Usage: make service-stop SERVICE=<service-name>"; \
		exit 1; \
	fi
	@echo "$(YELLOW)Stopping service: $(SERVICE)$(NC)"
	docker-compose -f $(COMPOSE_FILE) stop $(SERVICE)
	@echo "$(GREEN)Service $(SERVICE) stopped!$(NC)"

service-start:
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)ERROR: SERVICE parameter is required$(NC)"; \
		echo "Usage: make service-start SERVICE=<service-name>"; \
		exit 1; \
	fi
	@echo "$(GREEN)Starting service: $(SERVICE)$(NC)"
	docker-compose -f $(COMPOSE_FILE) start $(SERVICE)
	@echo "$(GREEN)Service $(SERVICE) started!$(NC)"

service-restart:
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)ERROR: SERVICE parameter is required$(NC)"; \
		echo "Usage: make service-restart SERVICE=<service-name>"; \
		exit 1; \
	fi
	@echo "$(YELLOW)Restarting service: $(SERVICE)$(NC)"
	docker-compose -f $(COMPOSE_FILE) restart $(SERVICE)
	@echo "$(GREEN)Service $(SERVICE) restarted!$(NC)"

# ============================================================================
# Legacy Commands (for non-Docker usage)
# ============================================================================

run: build start

build:
	@echo "$(GREEN)Building monolithic application...$(NC)"
	mvn clean package -DskipTests
	@echo "$(GREEN)Build completed!$(NC)"

start:
	@echo "$(GREEN)Starting Event Planner Monolith...$(NC)"
	@echo "$(YELLOW)Application will be available at: http://localhost:8080$(NC)"
	@echo "$(YELLOW)Auth health check: http://localhost:8080/api/v1/auth/health$(NC)"
	@echo "$(YELLOW)Press Ctrl+C to stop the application$(NC)"
	@echo ""
	@if [ -f .env ]; then \
		echo "Loading environment variables from .env file..."; \
		export $$(grep -v '^#' .env | grep -v '^$$' | xargs) && java -jar target/event-planner-monolith-1.0.0.jar; \
	else \
		echo "No .env file found, starting without environment variables..."; \
		java -jar target/event-planner-monolith-1.0.0.jar; \
	fi

stop:
	@echo "$(YELLOW)Stopping application...$(NC)"
	@pkill -f "java -jar target/event-planner-monolith-1.0.0.jar" || true
	@echo "$(GREEN)Application stopped!$(NC)"

restart: stop build start

logs:
	@echo "$(GREEN)Showing application logs...$(NC)"
	@tail -f logs/application.log 2>/dev/null || echo "$(RED)No log file found. Application may not be running.$(NC)"

clean:
	@echo "$(YELLOW)Cleaning build artifacts...$(NC)"
	mvn clean
	@echo "$(YELLOW)Stopping any running instances...$(NC)"
	@pkill -f "java -jar target/event-planner-monolith-1.0.0.jar" || true
	@echo "$(GREEN)Clean completed!$(NC)"

test:
	@./scripts/test.sh

postman: test

# Development mode with hot-reload (now using main compose file)
dev-up:
	@echo "$(GREEN)Starting services with hot-reload enabled...$(NC)"
	@echo "$(YELLOW)Code changes will be reflected immediately!$(NC)"
	@make compose-up
	@echo "$(YELLOW)Watching logs (Ctrl+C to exit)...$(NC)"
	@make compose-logs

dev-down:
	@make compose-down

dev-logs:
	@make compose-logs

dev-restart:
	@make compose-restart
