#!/bin/bash
# Buykon backend — buykon.com server
# Istifade: chmod +x start.sh && ./start.sh

set -e
cd "$(dirname "$0")"

JAR="bizdevar-backend.jar"
CONFIG="application.yml"
LOG="backend.log"
PORT=8080

if [ ! -f "$JAR" ]; then
  echo "XETA: $JAR tapilmadi. Eyni qovluqda olmalidir."
  exit 1
fi

if [ ! -f "$CONFIG" ]; then
  echo "XETA: $CONFIG tapilmadi."
  exit 1
fi

# Kohne proses varsa dayandir (8080 port xetasi olmasin)
OLD_PID=""
if command -v ss >/dev/null 2>&1; then
  OLD_PID=$(ss -tlnp 2>/dev/null | grep ":$PORT " | grep -oP 'pid=\K[0-9]+' | head -1)
fi
if [ -z "$OLD_PID" ] && pgrep -f "$JAR" >/dev/null 2>&1; then
  OLD_PID=$(pgrep -f "$JAR" | head -1)
fi

if [ -n "$OLD_PID" ]; then
  echo "Kohne backend tapildi (PID $OLD_PID) — dayandirilir..."
  kill "$OLD_PID" 2>/dev/null || true
  sleep 2
  if kill -0 "$OLD_PID" 2>/dev/null; then
    kill -9 "$OLD_PID" 2>/dev/null || true
    sleep 1
  fi
fi

echo "============================================"
echo "  Buykon Backend basladilir..."
echo "  Config: $(pwd)/$CONFIG"
echo "  DB:     buykondb @ localhost:3306"
echo "  API:    http://localhost:8080/api"
echo "  Log:    $(pwd)/$LOG"
echo "============================================"

nohup java -Xms256m -Xmx512m \
  -jar "$JAR" \
  --spring.config.location="file:$(pwd)/$CONFIG" \
  >> "$LOG" 2>&1 &

NEW_PID=$!
sleep 4

if kill -0 "$NEW_PID" 2>/dev/null; then
  echo "Backend isleyir (PID $NEW_PID)"
  curl -sf "http://127.0.0.1:$PORT/api/health" && echo "" || echo "Health yoxlamasi: bir nece saniye gozleyin"
else
  echo "XETA: Backend baslamadi. Log:"
  tail -20 "$LOG"
  exit 1
fi
