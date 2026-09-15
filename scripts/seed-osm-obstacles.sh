#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

set -a
source "$ROOT_DIR/.env"
set +a

DB_CONTAINER="${DB_CONTAINER:-goodroad_db}"

docker exec -i \
  -e PGPASSWORD="$DB_PASS" \
  "$DB_CONTAINER" \
  psql \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  -v ON_ERROR_STOP=1 \
  < "$SCRIPT_DIR/seed-osm-obstacles.sql"