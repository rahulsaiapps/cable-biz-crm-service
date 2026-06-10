#!/usr/bin/env bash
# Manual smoke-test helper for a running cable-crm-service instance.
#
# Required environment variables:
#   JWT_TOKEN   — Firebase ID token (never commit this value)
# Optional:
#   BASE_URL    — defaults to http://localhost:8080/api/v1
#   E2E_ID      — trace header UUID (auto-generated if unset)
#   SESSION_ID  — trace header UUID (auto-generated if unset)
#   LOCATION_ID — territory id for workspace customers probe
#
# Example:
#   export JWT_TOKEN="$(firebase auth:export ...)"  # or paste from app debug log
#   ./test_endpoints.sh

set -euo pipefail

if [ -z "$JWT_TOKEN" ]; then
  echo "Error: JWT_TOKEN env var is not set."
  exit 1
fi

TOKEN="${JWT_TOKEN}"

BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
E2E_ID="${E2E_ID:-$(uuidgen | tr '[:upper:]' '[:lower:]')}"
SESSION_ID="${SESSION_ID:-$(uuidgen | tr '[:upper:]' '[:lower:]')}"
LOCATION_ID="${LOCATION_ID:-ter_kolamuru_001}"

auth_headers=(
  -H "Authorization: Bearer ${TOKEN}"
  -H "X-E2E-ID: ${E2E_ID}"
  -H "X-Session-ID: ${SESSION_ID}"
)

echo "=================================="
echo "POST /auth/token-swap"
echo "=================================="
curl -sS -X POST "${BASE_URL}/auth/token-swap" \
  -H "Content-Type: application/json" \
  -d "{\"firebaseIdToken\":\"${TOKEN}\"}"
echo -e "\n"

echo "=================================="
echo "GET /workspace/customers?locationId=${LOCATION_ID}"
echo "=================================="
curl -sS -X GET "${BASE_URL}/workspace/customers?locationId=${LOCATION_ID}" \
  "${auth_headers[@]}"
echo -e "\n"

echo "=================================="
echo "GET /transactions/daily-summary"
echo "=================================="
curl -sS -X GET "${BASE_URL}/transactions/daily-summary?targetDate=$(date +%Y-%m-%d)" \
  "${auth_headers[@]}"
echo -e "\n"

echo "=================================="
echo "GET /auth/health"
echo "=================================="
curl -sS -X GET "${BASE_URL}/auth/health"
echo -e "\n=================================="
