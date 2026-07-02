#!/bin/bash
# Backend status
cd "$(dirname "$0")"

echo "=== Port 8080 ==="
ss -tlnp | grep ":8080 " || echo "Bosdur (backend islemir)"

echo ""
echo "=== Java proses ==="
pgrep -af "bizdevar-backend.jar" || echo "Tapilmadi"

echo ""
echo "=== Health ==="
curl -sf "http://127.0.0.1:8080/api/health" && echo "" || echo "Cavab yoxdur"

echo ""
echo "=== HTTPS (nginx) ==="
curl -sf "https://api.buykon.com/api/health" && echo "" || echo "Cavab yoxdur"
