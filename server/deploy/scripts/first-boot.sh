#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
ENV_FILE="${ENV_FILE:-$ROOT/.env}"
if [[ ! -f "$ENV_FILE" ]]; then
  cp "$ROOT/deploy/env.ecs.example" "$ENV_FILE"
  python3 - <<'PY' "$ENV_FILE"
import os, secrets, sys, pathlib
p = pathlib.Path(sys.argv[1])
text = p.read_text()
repl = {
    "replace-with-strong-password": secrets.token_urlsafe(18),
    "replace-with-strong-redis-password": secrets.token_urlsafe(18),
    "replace-with-at-least-32-character-secret": secrets.token_urlsafe(48),
    "replace-with-long-random-pepper": secrets.token_urlsafe(24),
    "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef": secrets.token_hex(32),
}
for a,b in repl.items():
    text = text.replace(a, b, 1)
p.write_text(text)
print("wrote", p)
PY
  echo "generated $ENV_FILE — keep this file off git"
fi
docker compose -f deploy/compose/docker-compose.all-in-one.yml --env-file "$ENV_FILE" up -d --build
echo "waiting for health"
for i in $(seq 1 40); do
  if curl -fsS http://127.0.0.1/health/live >/dev/null 2>&1; then
    echo "api is live on port 80"
    curl -fsS http://127.0.0.1/health/ready || true
    echo
    exit 0
  fi
  sleep 2
done
echo "timed out waiting for /health/live" >&2
exit 1
