#!/usr/bin/env bash
set -euo pipefail

base_url="${BASE_URL:-http://localhost:8080}"
alias_name="smoke-$(date +%s)"

response="$({
  curl --fail --silent --show-error \
    --header 'Content-Type: application/json' \
    --data "{\"url\":\"https://example.com/container-smoke\",\"customAlias\":\"${alias_name}\"}" \
    "${base_url}/shorten"
})"

code="$(printf '%s' "${response}" | sed -n 's/.*"code":"\([^"]*\)".*/\1/p')"
if [[ -z "${code}" ]]; then
  echo "Could not read a short code from: ${response}" >&2
  exit 1
fi

headers_file="$(mktemp)"
trap 'rm -f "${headers_file}"' EXIT

redirect_status="$(curl --silent --output /dev/null --dump-header "${headers_file}" --write-out '%{http_code}' "${base_url}/${code}")"
if [[ "${redirect_status}" != "301" ]]; then
  echo "Expected redirect status 301, received ${redirect_status}" >&2
  exit 1
fi

grep -qi '^location: https://example.com/container-smoke' "${headers_file}"

unknown_status="$(curl --silent --output /dev/null --write-out '%{http_code}' "${base_url}/definitely-missing")"
if [[ "${unknown_status}" != "404" ]]; then
  echo "Expected unknown-code status 404, received ${unknown_status}" >&2
  exit 1
fi

curl --fail --silent --show-error "${base_url}/api/links/${code}" >/dev/null
echo "Smoke test passed for ${base_url}/${code}"
