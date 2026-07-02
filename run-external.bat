@echo off
REM Buykon backend — xarici server (profile: external)
REM MySQL: buykondb / user: buykon

echo ============================================
echo   Buykon Backend - Xarici server
echo   Profile: external
echo   DB: buykondb @ localhost:3306
echo   API: http://localhost:8080/api
echo ============================================

cd /d "%~dp0"

set SPRING_PROFILES_ACTIVE=external

mvn spring-boot:run
