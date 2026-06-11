@echo off
echo =======================================================
echo    Starting GPS Cam Portal via Docker Containers   
echo =======================================================
echo.

echo Building and starting containers in the background...
docker compose up --build -d

timeout /t 3 /nobreak > nul

echo.
echo Docker containers are starting up! You can access them at:
echo -------------------------------------------------------
echo 🌐 Web Admin Portal : http://localhost:8080
echo ⚙️  Backend API      : http://localhost:5000
echo 🗄️  MySQL Database   : localhost:3306
echo -------------------------------------------------------
echo.
echo To view logs, run: docker compose logs -f
echo To stop servers, run: docker compose down
echo.
pause
