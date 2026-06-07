#!/usr/bin/env bash
set -euo pipefail

DB_CONTAINER=${DB_CONTAINER:-goodroad_db}
DB_NAME=${POSTGRES_DB:-goodroad}
DB_USER=${POSTGRES_USER:-goodroad}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < "$SCRIPT_DIR/seed-rewards-and-tasks.sql"
