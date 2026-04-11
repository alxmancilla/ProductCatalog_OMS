#!/usr/bin/env bash
# import-products.sh
#
# Uploads products-dataset.json to the running Spring Boot application.
# The API skips products whose SKU already exists (idempotent).
#
# Usage:
#   ./import-products.sh                         # uses defaults below
#   ./import-products.sh http://localhost:8080   # custom base URL
#   ./import-products.sh http://host:8080 /path/to/products.json

set -euo pipefail

API_BASE="${1:-http://localhost:8080}"
FILE="${2:-$(dirname "$0")/products-dataset.json}"

if [[ ! -f "$FILE" ]]; then
  echo "ERROR: File not found: $FILE" >&2
  exit 1
fi

echo "Importing products from: $FILE"
echo "Target: $API_BASE/products/import"
echo ""

curl --fail-with-body \
     --silent \
     --show-error \
     -X POST "$API_BASE/products/import" \
     -F "file=@$FILE" \
     -H "Accept: application/json" \
  | python3 -m json.tool 2>/dev/null || cat

echo ""
