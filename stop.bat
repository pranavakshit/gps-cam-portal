@echo off
echo =======================================================
echo    Stopping GPS Cam Portal via Docker Containers   
echo =======================================================
echo.

echo Stopping containers...
docker compose stop

echo.
echo Docker containers have been stopped successfully!
echo.
pause
