#!/usr/bin/env bash
# Get Auth0 access token via Resource Owner Password grant.
# Loads AUTH0_DOMAIN, AUTH0_CLIENT_ID, AUTH0_AUDIENCE (optional), SEED_USER1_EMAIL, SEED_USER1_PASSWORD
# from FE .env (SEED_FE_DIR) or current env. Outputs only the access_token to stdout.
set -euo pipefail
SEED_FE_DIR="${SEED_FE_DIR:-}"
if [[ -n "$SEED_FE_DIR" ]] && [[ -f "$SEED_FE_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$SEED_FE_DIR/.env"
  set +a
fi
DOMAIN="${AUTH0_DOMAIN:-}"
CLIENT_ID="${AUTH0_CLIENT_ID:-}"
AUDIENCE="${AUTH0_AUDIENCE:-}"
USERNAME="${SEED_USER1_EMAIL:-}"
PASSWORD="${SEED_USER1_PASSWORD:-SeedPass123!}"
if [[ -z "$DOMAIN" ]] || [[ -z "$CLIENT_ID" ]] || [[ -z "$USERNAME" ]] || [[ -z "$PASSWORD" ]]; then
  echo "Missing Auth0 or seed user config. Set in FE .env: AUTH0_DOMAIN, AUTH0_CLIENT_ID, SEED_USER1_EMAIL, SEED_USER1_PASSWORD" >&2
  exit 1
fi
DOMAIN="${DOMAIN#https://}"; DOMAIN="${DOMAIN#http://}"; DOMAIN="${DOMAIN%/}"
# Build JSON body (audience optional)
if [[ -n "$AUDIENCE" ]]; then
  JSON=$(jq -n --arg u "$USERNAME" --arg p "$PASSWORD" --arg c "$CLIENT_ID" --arg a "$AUDIENCE" \
    '{grant_type:"password",username:$u,password:$p,client_id:$c,scope:"openid profile email offline_access",audience:$a}')
else
  JSON=$(jq -n --arg u "$USERNAME" --arg p "$PASSWORD" --arg c "$CLIENT_ID" \
    '{grant_type:"password",username:$u,password:$p,client_id:$c,scope:"openid profile email offline_access"}')
fi
RESP=$(curl -sf -X POST "https://${DOMAIN}/oauth/token" \
  -H "Content-Type: application/json" \
  -d "$JSON") || exit 1
echo "$RESP" | jq -r '.access_token // empty'
if [[ -z "$(echo "$RESP" | jq -r '.access_token // empty')" ]]; then
  echo "Auth0 token response had no access_token. Check credentials and that the Password grant is enabled for this client." >&2
  echo "$RESP" | jq . >&2
  exit 1
fi
