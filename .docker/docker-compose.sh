#!/usr/bin/env bash
set -e

PROJECT_NAME="${COMPOSE_PROJECT_NAME:-voltradar}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"

echo "==> Running Docker Compose for project: '${PROJECT_NAME}'"

if [ $# -eq 0 ]; then
    docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" up -d
else
    docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" "$@"
fi
