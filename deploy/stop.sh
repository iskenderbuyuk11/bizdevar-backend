#!/bin/bash
# Backend dayandir
cd "$(dirname "$0")"

echo "Backend dayandirilir..."

pkill -f "bizdevar-backend.jar" 2>/dev/null && echo "Dayandirildi." || echo "Isleyen proses tapilmadi."

sleep 1
if ss -tlnp 2>/dev/null | grep -q ":8080 "; then
  PID=$(ss -tlnp | grep ":8080 " | grep -oP 'pid=\K[0-9]+' | head -1)
  if [ -n "$PID" ]; then
    kill -9 "$PID" 2>/dev/null || true
    echo "Port 8080 temizlendi (PID $PID)."
  fi
fi
