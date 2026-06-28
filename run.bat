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
mvn spring-boot:run
