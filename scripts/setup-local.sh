#!/usr/bin/env bash
set -euo pipefail

# ============================================
# Local Development Setup Script
# ============================================
# Sets up PostgreSQL + PostGIS natively and MinIO via Docker.
# Designed for macOS (Homebrew).
#
# Usage: ./scripts/setup-local.sh

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

DB_NAME="${DB_NAME:-shade_dev}"
DB_USER="${DB_USERNAME:-$(whoami)}"
PG_VERSION="16"

info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# --------------------------------------------------
# 1. Check / Install PostgreSQL
# --------------------------------------------------
setup_postgres() {
  info "Checking PostgreSQL..."

  if ! command -v psql &>/dev/null; then
    info "Installing PostgreSQL ${PG_VERSION} via Homebrew..."
    brew install "postgresql@${PG_VERSION}"
  else
    info "PostgreSQL already installed: $(psql --version)"
  fi

  # Start PostgreSQL service
  if ! brew services list | grep -q "postgresql.*started"; then
    info "Starting PostgreSQL service..."
    brew services start "postgresql@${PG_VERSION}" 2>/dev/null || brew services start postgresql 2>/dev/null || true
    sleep 2
  else
    info "PostgreSQL service is running."
  fi
}

# --------------------------------------------------
# 2. Check / Install PostGIS
# --------------------------------------------------
setup_postgis() {
  info "Checking PostGIS..."

  if ! brew list postgis &>/dev/null; then
    info "Installing PostGIS via Homebrew..."
    brew install postgis
  else
    info "PostGIS already installed."
  fi
}

# --------------------------------------------------
# 3. Create Database & Enable Extensions
# --------------------------------------------------
setup_database() {
  info "Setting up database '${DB_NAME}'..."

  # Create database if it doesn't exist
  if psql -lqt | cut -d \| -f 1 | grep -qw "${DB_NAME}"; then
    info "Database '${DB_NAME}' already exists."
  else
    info "Creating database '${DB_NAME}'..."
    createdb "${DB_NAME}"
    info "Database '${DB_NAME}' created."
  fi

  # Enable PostGIS extension
  info "Enabling PostGIS extension..."
  psql "${DB_NAME}" -c "CREATE EXTENSION IF NOT EXISTS postgis;" 2>/dev/null
  info "PostGIS extension enabled."

  # Verify
  PG_VERSION_OUT=$(psql "${DB_NAME}" -t -c "SELECT version();" | head -1 | xargs)
  POSTGIS_VERSION=$(psql "${DB_NAME}" -t -c "SELECT PostGIS_Version();" 2>/dev/null | head -1 | xargs)
  info "PostgreSQL: ${PG_VERSION_OUT}"
  info "PostGIS:    ${POSTGIS_VERSION}"
}

# --------------------------------------------------
# 4. Check / Install Redis
# --------------------------------------------------
setup_redis() {
  info "Checking Redis..."

  if ! command -v redis-server &>/dev/null; then
    info "Installing Redis via Homebrew..."
    brew install redis
  else
    info "Redis already installed: $(redis-server --version)"
  fi

  if ! brew services list | grep -q "redis.*started"; then
    info "Starting Redis service..."
    brew services start redis
    sleep 1
  else
    info "Redis service is running."
  fi
}

# --------------------------------------------------
# 5. Start MinIO + RabbitMQ via Docker Compose
# --------------------------------------------------
setup_docker_services() {
  info "Starting MinIO and RabbitMQ via Docker Compose..."

  if ! command -v docker &>/dev/null; then
    error "Docker is not installed. Please install Docker Desktop first."
    exit 1
  fi

  # Source .env.local if it exists for bucket names
  if [ -f ".env.local" ]; then
    set -a
    source .env.local
    set +a
  fi

  docker compose up -d rabbitmq minio minio-init

  info "Waiting for MinIO to be healthy..."
  sleep 5

  info "MinIO Console: http://localhost:9001 (minioadmin/minioadmin)"
  info "RabbitMQ Management: http://localhost:15672 (guest/guest)"
}

# --------------------------------------------------
# 6. Create .env.local if missing
# --------------------------------------------------
setup_env() {
  if [ ! -f ".env.local" ]; then
    warn ".env.local not found. Copy from template:"
    warn "  cp .env.local.example .env.local   (or use the one already committed)"
  else
    info ".env.local exists."
  fi
}

# --------------------------------------------------
# Main
# --------------------------------------------------
main() {
  echo ""
  echo "=========================================="
  echo "  SHADE — Local Development Setup"
  echo "=========================================="
  echo ""

  setup_env
  setup_postgres
  setup_postgis
  setup_database
  setup_redis
  setup_docker_services

  echo ""
  echo "=========================================="
  info "Local environment is ready!"
  echo "=========================================="
  echo ""
  info "Services running:"
  info "  PostgreSQL:  localhost:5432 (database: ${DB_NAME})"
  info "  Redis:       localhost:6379"
  info "  RabbitMQ:    localhost:5672 (management: http://localhost:15672)"
  info "  MinIO S3:    localhost:9000 (console: http://localhost:9001)"
  echo ""
  info "To start the Spring Boot app:"
  info "  source .env.local && mvn spring-boot:run"
  echo ""
  info "Or run everything in Docker:"
  info "  ENV_FILE=.env.local docker compose up -d"
  echo ""
}

main "$@"
