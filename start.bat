@echo off
echo =======================================================
echo    Starting GPS Cam Portal via Docker Containers   
echo =======================================================
echo.

echo Building and starting containers in the background...
docker compose up --build -d

timeout /t 3 /nobreak > nul

for /f "delims=" %%i in ('docker compose port web-portal 80') do set WEB_MAPPING=%%i
for /f "delims=" %%i in ('docker compose port backend 5001') do set API_MAPPING=%%i

set WEB_URL=http://%WEB_MAPPING:0.0.0.0=localhost%
set API_URL=http://%API_MAPPING:0.0.0.0=localhost%

echo.
echo Docker containers are starting up! You can access them at:
echo -------------------------------------------------------
echo 🌐 Web Admin Portal : %WEB_URL%
echo ⚙️  Backend API      : %API_URL%
echo 🗄️  MySQL Database   : localhost:3306
echo -------------------------------------------------------
echo.
echo To view logs, run: docker compose logs -f
echo To stop servers, run: docker compose down
echo.
pause
