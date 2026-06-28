#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

: "${DB_NAME:?DB_NAME is required}"
: "${DB_USER:?DB_USER is required}"
: "${DB_PASS:?DB_PASS is required}"

CONTAINER=${DB_CONTAINER:-goodroad_db}

docker cp scripts/test-data.sql "$CONTAINER:/tmp/goodroad-test-data.sql"
docker exec -e PGPASSWORD="$DB_PASS" "$CONTAINER" \
  psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -f /tmp/goodroad-test-data.sql

echo "GoodRoad test data loaded."
echo "Seed users: +79990000001 ... +79990000016; moderators: +79990000151 ... +79990000154; password: 123"
