@echo off
REM BizdeVar Java backend (Spring Boot) isesalma scripti.
REM Teleblar: JDK 17+, Maven (mvn), XAMPP-da MySQL isleyir olmalidir.

echo ============================================
echo   BizdeVar Backend - Spring Boot
echo   MySQL: localhost:3306 / baza: bizdevar
echo   API:   http://localhost:8080/api
echo   Admin: admin@bizdevar.com / Admin123
echo ============================================

cd /d "%~dp0"

REM 8080 portu doludursa, kohne backend prosesini dayandir
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
  echo Port 8080 isledilir - PID %%a dayandirilir...
  taskkill /PID %%a /F >nul 2>&1
)

mvn spring-boot:run
