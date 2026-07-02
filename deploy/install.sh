#!/bin/bash
# Buykon backend — tam qurasdirma (MySQL + schema + baslatma)
set -e
cd "$(dirname "$0")"

echo "=== 1/4 MySQL istifadeci ve baza ==="
if command -v mysql >/dev/null 2>&1; then
  sudo mysql < setup-mysql.sql
else
  echo "mysql tapilmadi — setup-mysql.sql-i el ile isledin"
fi

echo "=== 2/4 Cədvəllər (schema.sql) ==="
mysql -u buykon -p'isgender2008A1A2**' buykondb < schema.sql

echo "=== 3/4 Seed məlumat (data.sql) ==="
mysql -u buykon -p'isgender2008A1A2**' buykondb < data.sql

echo "=== 4/4 Backend basladilir ==="
chmod +x start.sh
./start.sh
