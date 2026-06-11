#!/bin/bash

echo -e "\033[1;32mStarting GPS Cam Portal Environment...\033[0m"
docker compose up -d

echo -e "\033[1;36mEnvironment is starting up in the background.\033[0m"
echo -e ""
echo -e "\033[1;35m==========================================\033[0m"
echo -e "\033[1;32m  Web Portal : http://localhost:5173\033[0m"
echo -e "\033[1;32m  Backend API: http://localhost:5000\033[0m"
echo -e "\033[1;32m  Database   : localhost:3307 (MySQL)\033[0m"
echo -e "\033[1;35m==========================================\033[0m"
echo -e ""
echo -e "\033[1;33mTailing logs... (Press 'q' at any time to gracefully stop)\033[0m"

# Tail logs in the background
docker compose logs -f &
LOG_PID=$!

# Save terminal settings
OLD_STTY=$(stty -g)

# Handle script interruption (CTRL+C)
trap 'stty $OLD_STTY; echo -e "\n\033[1;33mStopping environment gracefully...\033[0m"; kill $LOG_PID 2>/dev/null; docker compose stop; echo -e "\033[1;32mEnvironment stopped.\033[0m"; exit 0' SIGINT SIGTERM

# Read characters one by one without waiting for enter
stty -icanon time 0 min 0

while true; do
    read -n 1 -r key
    if [[ $key = "q" || $key = "Q" ]]; then
        break
    fi
    sleep 0.1
done

# Restore terminal behavior
stty $OLD_STTY

echo -e "\n\033[1;33mStopping environment gracefully...\033[0m"
kill $LOG_PID 2>/dev/null
docker compose stop
echo -e "\033[1;32mEnvironment stopped.\033[0m"
