param()

Write-Host "Starting GPS Cam Portal Environment..." -ForegroundColor Green
docker compose up -d

Write-Host "Environment is starting up in the background." -ForegroundColor Cyan
Write-Host ""
Write-Host "==========================================" -ForegroundColor Magenta
Write-Host "  Web Portal : http://localhost:5173" -ForegroundColor Green
Write-Host "  Backend API: http://localhost:5000" -ForegroundColor Green
Write-Host "  Database   : localhost:3307 (MySQL)" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Magenta
Write-Host ""
Write-Host "Tailing logs... (Press 'q' at any time to gracefully stop)" -ForegroundColor Yellow

$logJob = Start-Job {
    Set-Location $args[0]
    docker compose logs -f
} -ArgumentList $PWD

try {
    while ($true) {
        if ($Host.UI.RawUI.KeyAvailable) {
            $key = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
            if ($key.Character -eq 'q' -or $key.Character -eq 'Q') {
                break
            }
        }
        
        $events = Receive-Job $logJob
        foreach ($event in $events) {
            Write-Host $event
        }
        Start-Sleep -Milliseconds 100
    }
} finally {
    Write-Host "`nStopping environment gracefully..." -ForegroundColor Yellow
    Stop-Job $logJob
    Remove-Job $logJob
    docker compose stop
    Write-Host "Environment stopped." -ForegroundColor Green
}
