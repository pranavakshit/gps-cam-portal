Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "   Stopping GPS Cam Portal via Docker Containers   " -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Stopping containers..." -ForegroundColor Yellow
docker compose stop

Write-Host ""
Write-Host "Docker containers have been stopped successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Press any key to exit this script..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
