#!/usr/bin/env bash
set -euo pipefail
# Immutable image tag = Git SHA. Never deploy :latest.
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
IMAGE_TAG="${IMAGE_TAG:-$(git rev-parse HEAD)}"
IMAGE="xiaoquexing/xqx-api:${IMAGE_TAG}"
if [[ "$IMAGE_TAG" == "latest" ]]; then
  echo "refusing to deploy :latest" >&2
  exit 1
fi
echo "building $IMAGE"
docker build -t "$IMAGE" -f deploy/docker/Dockerfile .
export IMAGE_TAG
docker compose -f deploy/compose/docker-compose.ecs.yml up -d
echo "deployed $IMAGE"
curl -fsS "http://127.0.0.1:8080/health/live"
echo
