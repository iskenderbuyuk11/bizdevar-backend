@echo off
REM Lokal build — servere gondermek ucun paket yaradilir

cd /d "%~dp0\.."
echo Building bizdevar-backend.jar...
call mvn clean package -DskipTests -q
if errorlevel 1 (
  echo BUILD XETASI
  exit /b 1
)

copy /Y "target\bizdevar-backend.jar" "deploy\bizdevar-backend.jar"
copy /Y "src\main\resources\schema.sql" "deploy\schema.sql"
copy /Y "src\main\resources\data.sql" "deploy\data.sql"

echo.
echo Servere /root/ qovluguna yukleyin:
echo   deploy\bizdevar-backend.jar
echo   deploy\application.yml      ^(MUHUM^)
echo   deploy\start.sh
echo   deploy\setup-mysql.sql
echo   deploy\schema.sql
echo   deploy\data.sql
echo.
echo Sonra serverde: chmod +x start.sh ^&^& ./start.sh
