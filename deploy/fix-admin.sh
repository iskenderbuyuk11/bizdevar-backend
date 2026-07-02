#!/bin/bash
# Admin girisi 500 xetasi — serverde bir defe isledin
set -e
cd "$(dirname "$0")"

echo "=== Admin cedvelleri yaradilir ==="
mysql -u buykon -p buykondb < admin-tables.sql
echo "SQL tamamlandi."

echo "=== Backend yeniden basladilir ==="
./stop.sh 2>/dev/null || true
./start.sh

echo ""
echo "Yoxlama:"
sleep 3
curl -s "http://127.0.0.1:8080/api/health" && echo ""
curl -s "http://127.0.0.1:8080/api/auth/admin/ping" && echo ""
curl -s -X POST "http://127.0.0.1:8080/api/auth/admin/check-email" \
  -H "Content-Type: application/json" \
  -d '{"email":"isgenderpasayev@gmail.com"}' && echo ""
