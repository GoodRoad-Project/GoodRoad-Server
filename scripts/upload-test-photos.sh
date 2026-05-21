#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

: "${YC_BUCKET:?YC_BUCKET is required}"
: "${YC_ACCESS_KEY:?YC_ACCESS_KEY is required}"
: "${YC_SECRET_KEY:?YC_SECRET_KEY is required}"

if ! command -v aws >/dev/null 2>&1; then
  echo "aws cli is required for object storage seeding"
  exit 1
fi

export AWS_ACCESS_KEY_ID="$YC_ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="$YC_SECRET_KEY"
export AWS_DEFAULT_REGION="ru-central1"
ENDPOINT=${YC_ENDPOINT:-https://storage.yandexcloud.net}

aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/101/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/103/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/104/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/106/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/107/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/109/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/110/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/112/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/113/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/115/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/116/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/151/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/152/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/avatar.png "s3://$YC_BUCKET/avatars/154/avatar.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/stairs.png "s3://$YC_BUCKET/reviews/101/review-301-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/stairs.png "s3://$YC_BUCKET/reviews/101/review-301-2.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/potholes.png "s3://$YC_BUCKET/reviews/108/review-303-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/stairs.png "s3://$YC_BUCKET/reviews/115/review-305-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/curb.png "s3://$YC_BUCKET/reviews/106/review-307-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/stairs.png "s3://$YC_BUCKET/reviews/109/review-308-2.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/potholes.png "s3://$YC_BUCKET/reviews/113/review-309-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/stairs.png "s3://$YC_BUCKET/reviews/104/review-311-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/curb.png "s3://$YC_BUCKET/reviews/111/review-313-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/potholes.png "s3://$YC_BUCKET/reviews/102/review-315-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/potholes.png "s3://$YC_BUCKET/reviews/102/review-315-2.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/stairs.png "s3://$YC_BUCKET/reviews/109/review-317-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/stairs.png "s3://$YC_BUCKET/reviews/116/review-319-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/potholes.png "s3://$YC_BUCKET/reviews/107/review-321-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/curb.png "s3://$YC_BUCKET/reviews/110/review-322-2.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/stairs.png "s3://$YC_BUCKET/reviews/114/review-323-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/curb.png "s3://$YC_BUCKET/reviews/105/review-325-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/potholes.png "s3://$YC_BUCKET/reviews/112/review-327-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/stairs.png "s3://$YC_BUCKET/reviews/103/review-329-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/stairs.png "s3://$YC_BUCKET/reviews/103/review-329-2.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/curb.png "s3://$YC_BUCKET/reviews/110/review-331-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/potholes.png "s3://$YC_BUCKET/reviews/101/review-333-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/stairs.png "s3://$YC_BUCKET/reviews/108/review-335-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/curb.png "s3://$YC_BUCKET/reviews/111/review-336-2.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/stairs.png "s3://$YC_BUCKET/reviews/115/review-337-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/potholes.png "s3://$YC_BUCKET/reviews/106/review-339-1.png" --content-type image/png
aws --endpoint-url "$ENDPOINT" s3 cp scripts/test-photos/stairs.png "s3://$YC_BUCKET/reviews/113/review-341-1.png" --content-type image/png

echo "GoodRoad test photos uploaded to $YC_BUCKET."
