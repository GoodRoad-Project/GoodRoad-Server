#!/bin/sh

set -e

MAP_FILE="/app/data/northwestern-fed-district.osm.pbf"
S3_OBJECT="northwestern-fed-district.osm.pbf"

if [ -f "$MAP_FILE" ]; then
    echo "OSM map already exists: $MAP_FILE"
    exit 0
fi

if [ -z "$YC_ACCESS_KEY" ] || [ -z "$YC_SECRET_KEY" ] || [ -z "$YC_BUCKET" ]; then
    echo "ERROR: YC_ACCESS_KEY, YC_SECRET_KEY and YC_BUCKET must be set"
    exit 1
fi

mkdir -p /app/data

echo "OSM map not found."
echo "Downloading s3://$YC_BUCKET/$S3_OBJECT ..."

export AWS_ACCESS_KEY_ID="$YC_ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="$YC_SECRET_KEY"
export AWS_DEFAULT_REGION="ru-central1"

aws s3 cp \
    "s3://$YC_BUCKET/$S3_OBJECT" \
    "$MAP_FILE" \
    --endpoint-url "https://storage.yandexcloud.net"

echo "OSM map downloaded successfully: $MAP_FILE"