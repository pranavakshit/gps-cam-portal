@echo off
echo =======================================================
echo       Starting GPS Cam Portal Development Servers      
echo =======================================================
echo.

echo [1/2] Starting Backend Server...
start "GPS Cam Backend" cmd /c "cd backend && npm run dev"

echo [2/2] Starting Web Portal...
start "GPS Cam Web Portal" cmd /c "cd web-portal && npm run dev"

timeout /t 3 /nobreak > nul

echo.
echo Servers are starting up! You can access them at:
echo -------------------------------------------------------
echo 🌐 Web Admin Portal : http://localhost:5173
echo ⚙️  Backend API      : http://localhost:5000
echo -------------------------------------------------------
echo.
pause
