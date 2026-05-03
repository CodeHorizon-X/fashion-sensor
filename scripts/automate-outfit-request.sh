#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_URL="http://127.0.0.1:3000/frontend/index.html"
BACKEND_HEALTH_URL="http://127.0.0.1:8081/api/explore?audience=men&style=casual"
AGENTIC_URL="http://127.0.0.1:8081/api/agentic-suggest"

BACKEND_PID=""
FRONTEND_PID=""

cleanup() {
  if [[ -n "$FRONTEND_PID" ]] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
    kill "$FRONTEND_PID" 2>/dev/null || true
  fi
  if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    kill "$BACKEND_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

wait_for_url() {
  local url="$1"
  local label="$2"
  local max_attempts="${3:-60}"

  for ((attempt = 1; attempt <= max_attempts; attempt++)); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "$label is ready."
      return 0
    fi
    sleep 1
  done

  echo "Timed out waiting for $label at $url" >&2
  return 1
}

open_browser() {
  local url="$1"
  if command -v open >/dev/null 2>&1; then
    open "$url"
  elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$url" >/dev/null 2>&1 &
  else
    echo "Open this URL manually: $url"
  fi
}

echo "Starting backend..."
(
  cd "$BACKEND_DIR"
  ./mvnw spring-boot:run
) &
BACKEND_PID="$!"

wait_for_url "$BACKEND_HEALTH_URL" "Backend"

echo "Starting frontend static server on port 3000..."
(
  cd "$ROOT_DIR"
  python3 -m http.server 3000
) &
FRONTEND_PID="$!"

wait_for_url "$FRONTEND_URL" "Frontend"

echo "Opening browser at $FRONTEND_URL"
open_browser "$FRONTEND_URL"

echo "Triggering exactly one outfit request..."
curl -fsS "$AGENTIC_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "gender": "men",
    "purpose": "casual",
    "styleVibe": "casual",
    "location": "Mumbai",
    "notes": "comfortable and breathable",
    "history": []
  }'
echo

echo "Automation finished. Backend and frontend stay running until you press Ctrl-C."
wait "$BACKEND_PID"
