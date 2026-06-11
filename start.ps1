Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "      Starting GPS Cam Portal Development Servers      " -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[1/2] Starting Backend Server..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend; npm run dev" -WindowStyle Normal

Write-Host "[2/2] Starting Web Portal..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd web-portal; npm run dev" -WindowStyle Normal

Start-Sleep -Seconds 3

Write-Host ""
Write-Host "Servers are starting up! You can access them at:" -ForegroundColor Green
Write-Host "-------------------------------------------------------"
Write-Host "🌐 Web Admin Portal : http://localhost:5173" -ForegroundColor Cyan
Write-Host "⚙️  Backend API      : http://localhost:5000" -ForegroundColor Cyan
Write-Host "-------------------------------------------------------"
Write-Host ""
Write-Host "Press any key to exit this script (the servers will keep running in their own windows)..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
