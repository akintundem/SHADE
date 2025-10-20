#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSISTANT_DIR="$ROOT_DIR/shade-assistant"
VENV_DIR="$ASSISTANT_DIR/.venv"
LOG_DIR="$ROOT_DIR/logs"
ASSISTANT_LOG="$LOG_DIR/shade-assistant.log"

mkdir -p "$LOG_DIR"

if ! command -v docker-compose >/dev/null 2>&1; then
  echo "docker-compose is required but not installed. Please install docker-compose." >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required but not installed. Please install Python 3.11+." >&2
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "mvn (Maven) is required but not installed. Please install Maven 3.9+." >&2
  exit 1
fi

if [ ! -f "$ROOT_DIR/.env" ]; then
  echo "Missing .env in project root. Copy env.template to .env and configure credentials." >&2
  exit 1
fi

if [ ! -f "$ASSISTANT_DIR/.env" ]; then
  echo "Warning: shade-assistant/.env not found. The assistant may not start without OPENAI_API_KEY." >&2
fi

echo "Starting supporting services (Postgres, Redis)..."
docker-compose -f "$ROOT_DIR/docker-compose.yml" up -d postgres redis >/dev/null

wait_for_port() {
  local host=$1
  local port=$2
  local name=$3
  echo "Waiting for $name on ${host}:${port}..."
  for attempt in {1..40}; do
    if nc -z "$host" "$port" >/dev/null 2>&1; then
      echo "$name is available."
      return 0
    fi
    sleep 2
  done
  echo "Timed out waiting for $name on ${host}:${port}." >&2
  exit 1
}

wait_for_port "localhost" 5432 "Postgres"
wait_for_port "localhost" 6379 "Redis"

if [ ! -d "$VENV_DIR" ]; then
  echo "Creating Python virtual environment..."
  python3 -m venv "$VENV_DIR"
fi

echo "Installing Python dependencies..."
# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"
pip install --upgrade pip >/dev/null
pip install -r "$ASSISTANT_DIR/requirements.txt"

echo "Launching LangChain assistant (logs: $ASSISTANT_LOG)..."
pushd "$ASSISTANT_DIR" >/dev/null
uvicorn app.main:app --host 0.0.0.0 --port 9000 --log-level info >"$ASSISTANT_LOG" 2>&1 &
ASSISTANT_PID=$!
popd >/dev/null
deactivate

cleanup() {
  echo ""
  echo "Shutting down services..."
  if ps -p "$ASSISTANT_PID" >/dev/null 2>&1; then
    kill "$ASSISTANT_PID" >/dev/null 2>&1 || true
    wait "$ASSISTANT_PID" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

echo "LangChain assistant PID: $ASSISTANT_PID"
echo "Launching Spring Boot application..."

pushd "$ROOT_DIR" >/dev/null
mvn spring-boot:run
popd >/dev/null
