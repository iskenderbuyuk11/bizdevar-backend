-- Buykon server — MySQL ilkin qurasdirma
-- Calistir: sudo mysql < setup-mysql.sql

CREATE DATABASE IF NOT EXISTS buykondb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'buykon'@'localhost' IDENTIFIED BY 'isgender2008A1A2**';
GRANT ALL PRIVILEGES ON buykondb.* TO 'buykon'@'localhost';
FLUSH PRIVILEGES;
