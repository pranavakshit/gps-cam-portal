# GPS Cam Portal

A comprehensive solution consisting of an offline-first Android application that captures GPS-stamped photos, and a full-stack web portal (React + Node.js + MySQL) to manage and view those uploads on a dashboard.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop) (for running the database and web services)
- [Node.js 18+](https://nodejs.org/) (if running manually without Docker)
- [Android Studio](https://developer.android.com/studio) (for building the Android app)

## 🚀 Starting the Web Portal & Server

The entire web stack (MySQL Database, Express Backend, and React Frontend) has been fully Dockerized for development. It includes live hot-reloading.

Simply run the unified startup script for your platform from the root directory:

**Windows:**
```powershell
# Using PowerShell
.\start.ps1

# Or using Command Prompt
start.bat
```

**Linux / macOS:**
```bash
./start.sh
```

These scripts will automatically:
1. Spin up the MySQL, Backend, and Frontend containers.
2. Run database migrations and seed the default admin account.
3. Print the local URLs to access the portal.
4. Tail the live logs until you press **`q`** to gracefully shut everything down.

### Accessing the Portal
Once running, open your browser to:
- **Web Portal:** [http://localhost:5173](http://localhost:5173)
- **Backend API:** [http://localhost:5000](http://localhost:5000)

**Default Login:**
- Username: `admin`
- Password: `password` (or any string, depending on your environment seeding)

---

## 📱 Setting up the Android App

The Android app captures photos with embedded GPS coordinates and uploads them to the backend.

1. **Open the Project:**
   Open the `android-app` directory in **Android Studio**.

2. **Configure Backend IP (If testing on a physical device):**
   By default, the app is configured to communicate with the Android Emulator's loopback address (`http://10.0.2.2:5000`). 
   If you are installing the app on a **real physical device**, you must update the `BASE_URL` in `android-app/app/src/main/java/com/pranavakshit/gpscamportal/data/remote/ApiService.kt` to point to your computer's local Wi-Fi IPv4 address (e.g., `http://192.168.x.x:5000`).

3. **Build and Run:**
   Connect your device or start an emulator, and click the **Run** button (Shift + F10) in Android Studio.

4. **Usage:**
   - Grant the necessary Camera and Location permissions.
   - Snap photos on the main screen.
   - Navigate to the **Uploads** tab to sync your captured photos to the running backend.

---

## License

This software is licensed under the **PolyForm Noncommercial License 1.0.0**. 

You may not use this software for any commercial purposes (e.g., selling it, providing commercial services, or incorporating it into a commercial product). Please see the `LICENSE` file for full details and liability disclaimers.