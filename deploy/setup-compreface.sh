#!/bin/bash
# CompreFace quraşdırması — Buykon backend server
set -e

CF_DIR="/opt/compreface"
CF_PORT=8000
ADMIN_EMAIL="${CF_ADMIN_EMAIL:-compreface@buykon.com}"
ADMIN_PASS="${CF_ADMIN_PASS:-BuykonCf2026!Secure}"

echo "=== CompreFace quraşdırılır ==="

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker quraşdırılır..."
  curl -fsSL https://get.docker.com | sh
  systemctl enable docker
  systemctl start docker
fi

if ! docker compose version >/dev/null 2>&1 && ! docker-compose version >/dev/null 2>&1; then
  apt-get update -qq && apt-get install -y docker-compose-plugin 2>/dev/null || true
fi

COMPOSE="docker compose"
$COMPOSE version >/dev/null 2>&1 || COMPOSE="docker-compose"

mkdir -p "$CF_DIR"
cd "$CF_DIR"

if [ ! -f docker-compose.yml ]; then
  curl -fsSL -o docker-compose.yml \
    "https://raw.githubusercontent.com/exadel-inc/CompreFace/master/docker-compose.yml"
  curl -fsSL -o .env \
    "https://raw.githubusercontent.com/exadel-inc/CompreFace/master/.env"
fi

$COMPOSE pull -q 2>/dev/null || true
$COMPOSE up -d

echo "CompreFace konteynerləri gözlənilir..."
for i in $(seq 1 60); do
  if curl -sf "http://127.0.0.1:${CF_PORT}/api/v1/recognition/subjects" -H "x-api-key: test" >/dev/null 2>&1 \
     || curl -sf "http://127.0.0.1:${CF_PORT}/" >/dev/null 2>&1; then
    break
  fi
  sleep 5
done
sleep 10

# Admin qeydiyyat (ilk dəfə)
curl -sf -X POST "http://127.0.0.1:${CF_PORT}/api/v1/admin/user/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${ADMIN_PASS}\",\"firstName\":\"Buykon\",\"lastName\":\"Admin\"}" \
  >/dev/null 2>&1 || true

# OAuth token
TOKEN_RESP=$(curl -sf -X POST "http://127.0.0.1:${CF_PORT}/admin/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=${ADMIN_EMAIL}&password=${ADMIN_PASS}" 2>/dev/null || echo "")

ACCESS_TOKEN=$(echo "$TOKEN_RESP" | grep -o '"access_token":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
  echo "XETA: CompreFace admin token alinmadi. UI: http://SERVER:${CF_PORT}"
  exit 1
fi

# Application yarat
APP_RESP=$(curl -sf -X POST "http://127.0.0.1:${CF_PORT}/api/v1/admin/applications" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"name":"BuykonSeller"}' 2>/dev/null || echo "")

APP_ID=$(echo "$APP_RESP" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$APP_ID" ]; then
  # Mövcud app
  APPS=$(curl -sf "http://127.0.0.1:${CF_PORT}/api/v1/admin/applications" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" 2>/dev/null || echo "[]")
  APP_ID=$(echo "$APPS" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
fi

# Recognition service + API key
SVC_RESP=$(curl -sf -X POST "http://127.0.0.1:${CF_PORT}/api/v1/admin/applications/${APP_ID}/models" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"name":"SellerFace","type":"RECOGNITION"}' 2>/dev/null || echo "")

API_KEY=$(echo "$SVC_RESP" | grep -o '"apiKey":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$API_KEY" ]; then
  MODELS=$(curl -sf "http://127.0.0.1:${CF_PORT}/api/v1/admin/applications/${APP_ID}/models" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" 2>/dev/null || echo "[]")
  API_KEY=$(echo "$MODELS" | grep -o '"apiKey":"[^"]*"' | head -1 | cut -d'"' -f4)
fi

if [ -z "$API_KEY" ]; then
  echo "XETA: API key alinmadi. CompreFace UI: http://127.0.0.1:${CF_PORT}"
  exit 1
fi

echo "CompreFace API key: ${API_KEY}"

# application.yml yenilə
APP_YML="/root/application.yml"
if [ -f "$APP_YML" ]; then
  if grep -q "compreface:" "$APP_YML"; then
    sed -i "s/enabled: .*/enabled: true/" "$APP_YML" 2>/dev/null || true
    sed -i "s|url: .*|url: http://127.0.0.1:${CF_PORT}|" "$APP_YML" 2>/dev/null || true
    sed -i "s/api-key: .*/api-key: ${API_KEY}/" "$APP_YML" 2>/dev/null || true
  else
    cat >> "$APP_YML" <<EOF

  compreface:
    enabled: true
    url: http://127.0.0.1:${CF_PORT}
    api-key: ${API_KEY}
    similarity-threshold: 0.85
EOF
  fi
  echo "application.yml yenilendi"
fi

echo "=== CompreFace hazirdir ==="
