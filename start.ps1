Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "   Starting GPS Cam Portal via Docker Containers   " -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Building and starting containers in the background..." -ForegroundColor Yellow
docker compose up --build -d

Start-Sleep -Seconds 3

# Fetch the dynamically assigned host ports
$WebPortMapping = docker compose port web-portal 80
$ApiPortMapping = docker compose port backend 5001

$WebUrl = "http://" + ($WebPortMapping -replace '0.0.0.0', 'localhost')
$ApiUrl = "http://" + ($ApiPortMapping -replace '0.0.0.0', 'localhost')

Write-Host ""
Write-Host "Docker containers are starting up! You can access them at:" -ForegroundColor Green
Write-Host "-------------------------------------------------------"
Write-Host "🌐 Web Admin Portal : $WebUrl" -ForegroundColor Cyan
Write-Host "⚙️  Backend API      : $ApiUrl" -ForegroundColor Cyan
Write-Host "🗄️  MySQL Database   : localhost:3306" -ForegroundColor Cyan
Write-Host "-------------------------------------------------------"
Write-Host ""
Write-Host "To view logs, run: docker compose logs -f"
Write-Host "To stop servers, run: docker compose down"
Write-Host ""
Write-Host "Press any key to exit this script..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
