#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_ROOT="$ROOT_DIR/test-results"
RUN_DIR="$LOG_ROOT/latest"
SERVER_LOG="$RUN_DIR/server.log"
EXPORTED_ENV="$RUN_DIR/postman-env.json"
AUTH_COLLECTION="$ROOT_DIR/Postman Collections/Event_Planner_Auth_Service_Testing.postman_collection.json"
EVENT_COLLECTION="$ROOT_DIR/Postman Collections/Event_Planner_Event_Service_Testing.postman_collection.json"
BASE_ENVIRONMENT="$ROOT_DIR/test-environment.json"
APP_JAR="$ROOT_DIR/target/event-planner-monolith-1.0.0.jar"
HEALTH_URL="http://localhost:8080/actuator/health"

mkdir -p "$RUN_DIR"

INFRA_STARTED=0
SERVER_PID=""

cleanup() {
  local exit_code=$?
  if [[ -n "${SERVER_PID}" ]] && kill -0 "${SERVER_PID}" 2>/dev/null; then
    echo "Shutting down application (pid: ${SERVER_PID})..."
    kill "${SERVER_PID}" 2>/dev/null || true
    wait "${SERVER_PID}" 2>/dev/null || true
  fi

  if [[ $INFRA_STARTED -eq 1 ]]; then
    echo "Stopping Docker services..."
    (cd "$ROOT_DIR" && docker-compose down >/dev/null 2>&1 || true)
  fi

  exit "$exit_code"
}
trap cleanup EXIT

echo "Starting test infrastructure..."
(cd "$ROOT_DIR" && docker-compose up -d postgres redis)
INFRA_STARTED=1

echo "Cleaning previous build artifacts..."
rm -rf "$ROOT_DIR/target"

echo "Building application jar..."
(cd "$ROOT_DIR" && mvn clean package -DskipTests)

if [[ ! -f "$APP_JAR" ]]; then
  echo "Unable to locate application jar at $APP_JAR" >&2
  exit 1
fi

echo "Launching application in background..."
java -jar "$APP_JAR" >"$SERVER_LOG" 2>&1 &
SERVER_PID=$!

echo "Waiting for application health check at $HEALTH_URL ..."
for attempt in {1..30}; do
  if curl -sf "$HEALTH_URL" >/dev/null 2>&1; then
    echo "Application is healthy."
    break
  fi
  if [[ $attempt -eq 30 ]]; then
    echo "Application failed to become healthy in time. Check $SERVER_LOG for details." >&2
    exit 1
  fi
  sleep 2
done

echo "Running Auth Postman collection..."
newman run "$AUTH_COLLECTION" -e "$BASE_ENVIRONMENT" --export-environment "$EXPORTED_ENV"

echo "Running Event Postman collection..."
newman run "$EVENT_COLLECTION" -e "$EXPORTED_ENV"

echo "All Postman collections executed successfully."
echo "Logs available at $SERVER_LOG"
