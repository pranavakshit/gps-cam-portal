Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "   Starting GPS Cam Portal via Docker Containers   " -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Building and starting containers in the background..." -ForegroundColor Yellow
docker compose up --build -d

Start-Sleep -Seconds 3

Write-Host ""
Write-Host "Docker containers are starting up! You can access them at:" -ForegroundColor Green
Write-Host "-------------------------------------------------------"
Write-Host "🌐 Web Admin Portal : http://localhost:5173" -ForegroundColor Cyan
Write-Host "⚙️  Backend API      : http://localhost:5001" -ForegroundColor Cyan
Write-Host "🗄️  MySQL Database   : localhost:3306" -ForegroundColor Cyan
Write-Host "-------------------------------------------------------"
Write-Host ""
Write-Host "To view logs, run: docker compose logs -f"
Write-Host "To stop servers, run: docker compose down"
Write-Host ""
Write-Host "Press any key to exit this script..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
