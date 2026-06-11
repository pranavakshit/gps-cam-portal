@echo off
setlocal

echo Starting GPS Cam Portal Environment...
docker compose up -d

echo Environment is starting up in the background.
echo Tailing logs... (Press 'q' at any time to gracefully stop)

:: Run logs in background. It will automatically terminate when containers stop.
start /B docker compose logs -f

:loop
:: Wait for 'q' keypress
choice /c q /n /m ""
if errorlevel 1 goto stop

:stop
echo.
echo Stopping environment gracefully...
docker compose stop
echo Environment stopped.
