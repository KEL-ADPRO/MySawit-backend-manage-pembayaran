#!/usr/bin/env bash
# Run Grafana Alloy locally to scrape the app and push metrics to Grafana Cloud.
# Usage: ./run-alloy.sh
# Requires: docker, and .env file in this directory (copy from .env.example).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"
CONFIG_FILE="${SCRIPT_DIR}/alloy-config.alloy"

if [[ ! -f "${ENV_FILE}" ]]; then
	echo "ERROR: ${ENV_FILE} not found." >&2
	echo "       cp ${SCRIPT_DIR}/env.example ${ENV_FILE}  → lalu isi kredensialnya." >&2
	exit 1
fi

if ! docker info >/dev/null 2>&1; then
	echo "ERROR: Docker daemon tidak responding. Buka Docker Desktop dulu." >&2
	exit 1
fi

echo "→ Pulling grafana/alloy:latest (skip kalau udah ada)..."
docker pull grafana/alloy:latest

echo "→ Removing previous container kalau ada..."
docker rm -f mysawit-alloy >/dev/null 2>&1 || true

echo "→ Starting Alloy..."
docker run -d \
	--name mysawit-alloy \
	--restart unless-stopped \
	--add-host=host.docker.internal:host-gateway \
	--env-file "${ENV_FILE}" \
	-v "${CONFIG_FILE}":/etc/alloy/config.alloy:ro \
	-p 12345:12345 \
	grafana/alloy:latest \
	run --server.http.listen-addr=0.0.0.0:12345 \
	/etc/alloy/config.alloy

echo ""
echo "✅ Alloy started."
echo "   Logs:    docker logs -f mysawit-alloy"
echo "   UI:      http://localhost:12345"
echo "   Stop:    docker rm -f mysawit-alloy"
