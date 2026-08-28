#!/usr/bin/env bash
set -euo pipefail
# Rollback to a previously built SHA tag. IMAGE_TAG is required.
if [[ -z "${IMAGE_TAG:-}" || "$IMAGE_TAG" == "latest" ]]; then
  echo "IMAGE_TAG=<git-sha> is required" >&2
  exit 1
fi
export IMAGE_TAG
docker compose -f deploy/compose/docker-compose.ecs.yml up -d
curl -fsS "http://127.0.0.1:8080/health/live"
echo
echo "rolled back to xiaoquexing/xqx-api:${IMAGE_TAG}"
