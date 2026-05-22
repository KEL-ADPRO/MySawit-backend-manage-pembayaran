#!/usr/bin/env bash
# Run Grafana Alloy NATIVELY (tanpa Docker) - butuh `alloy` udah di-install lewat Homebrew.
# Usage: ./run-alloy-native.sh
# Stop: Ctrl+C (foreground), atau kill via Activity Monitor

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"
CONFIG_FILE="${SCRIPT_DIR}/alloy-config.alloy"

if ! command -v alloy >/dev/null 2>&1; then
	echo "ERROR: alloy belum ke-install." >&2
	echo "       brew install grafana/grafana/alloy" >&2
	exit 1
fi

if [[ ! -f "${ENV_FILE}" ]]; then
	echo "ERROR: ${ENV_FILE} not found." >&2
	exit 1
fi

# Load env vars dari .env
set -a
# shellcheck disable=SC1090
source "${ENV_FILE}"
set +a

# Alloy native pakai host networking, jadi target lokal langsung pakai localhost.
# Kalau target dari .env masih host.docker.internal, ganti ke localhost.
if [[ "${MYSAWIT_APP_TARGET}" == host.docker.internal:* ]]; then
	export MYSAWIT_APP_TARGET="${MYSAWIT_APP_TARGET/host.docker.internal/localhost}"
fi

echo "→ Starting Alloy (foreground). Tekan Ctrl+C untuk stop."
echo "   Target:   ${MYSAWIT_APP_TARGET}"
echo "   UI:       http://localhost:12345"
echo ""

exec alloy run \
	--server.http.listen-addr=0.0.0.0:12345 \
	"${CONFIG_FILE}"
